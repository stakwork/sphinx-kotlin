package chat.sphinx.feature_socket_io

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_client.NetworkClientClearedListener
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.retrieveRelayUrlAndToken
import chat.sphinx.concept_socket_io.*
import chat.sphinx.feature_socket_io.json.MessageResponse
import chat.sphinx.feature_socket_io.json.getMessageResponse
import chat.sphinx.feature_socket_io.json.getMessageType
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.logger.w
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.socket.client.client.IO
import io.socket.client.client.Manager
import io.socket.client.client.Socket
import io.socket.engine.engineio.client.EngineIOException
import io.socket.engine.engineio.client.Socket as EngineSocket
import io.socket.engine.engineio.client.Transport
import io.socket.engine.engineio.client.transports.Polling
import io.socket.engine.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.collections.LinkedHashSet

class SocketIOManagerImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler,
    private val LOG: SphinxLogger,
) : SocketIOManager(),
    NetworkClientClearedListener,
    CoroutineDispatchers by dispatchers
{

    companion object {
        const val TAG = "SocketIOManagerImpl"
    }

    /////////////////////////
    /// State/Error Flows ///
    /////////////////////////
    @Suppress("RemoveExplicitTypeArguments")
    private val _socketIOStateFlow: MutableStateFlow<SocketIOState> by lazy {
        MutableStateFlow<SocketIOState>(SocketIOState.Uninitialized)
    }
    override val socketIOStateFlow: StateFlow<SocketIOState>
        get() = _socketIOStateFlow.asStateFlow()

    @Suppress("RemoveExplicitTypeArguments")
    private val _socketIOSharedFlow: MutableSharedFlow<SocketIOError> by lazy {
        MutableSharedFlow<SocketIOError>(0, 1)
    }
    override val socketIOErrorFlow: SharedFlow<SocketIOError>
        get() = _socketIOSharedFlow.asSharedFlow()

    /////////////////
    /// Listeners ///
    /////////////////
    private inner class SynchronizedListenerHolder {
        private val listeners: LinkedHashSet<SphinxSocketIOMessageListener> = LinkedHashSet(0)

        fun addListener(listener: SphinxSocketIOMessageListener): Boolean =
            synchronized(this) {
                val bool = listeners.add(listener)
                if (bool) {
                    LOG.d(TAG, "Listener ${listener.javaClass.simpleName} registered")
                }
                return bool
            }

        fun removeListener(listener: SphinxSocketIOMessageListener): Boolean =
            synchronized(this) {
                val bool = listeners.remove(listener)
                if (bool) {
                    LOG.d(TAG, "Listener ${listener.javaClass.simpleName} removed")
                }
                return bool
            }

        fun clear() {
            synchronized(this) {
                if (listeners.isNotEmpty()) {
                    listeners.clear()
                    LOG.d(TAG, "Listeners cleared")
                }
            }
        }

        fun dispatch(msg: SphinxSocketIOMessage) {
            synchronized(this) {
                for (listener in listeners) {
                    instance?.socketIOScope?.launch(io) {
                        try {
                            listener.onSocketIOMessageReceived(msg)
                        } catch (e: Exception) {
                            LOG.e(
                                TAG,
                                "Listener ${listener.javaClass.simpleName} threw exception " +
                                "${e.javaClass.simpleName} for type ${msg.javaClass.simpleName}",
                                e
                            )
                        }
                    } ?: LOG.w(
                        TAG,
                        """
                            EVENT_MESSAGE: type ${msg.javaClass.simpleName}
                            SocketIOState: ${_socketIOStateFlow.value}
                            Instance: >>> null <<<
                        """.trimIndent()
                    )
                }
            }
        }

        val hasListeners: Boolean
            get() = synchronized(this) {
                listeners.isNotEmpty()
            }
    }

    private val synchronizedListeners = SynchronizedListenerHolder()

    override fun addListener(listener: SphinxSocketIOMessageListener): Boolean {
        return synchronizedListeners.addListener(listener)
    }

    override fun removeListener(listener: SphinxSocketIOMessageListener): Boolean {
        return synchronizedListeners.removeListener(listener)
    }

    /////////////////////
    /// Socket/Client ///
    /////////////////////
    private class SocketInstanceHolder(
        val socket: Socket,
        val socketIOClient: OkHttpClient,
        val relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>,
        val socketIOSupervisor: Job = SupervisorJob(),
        val socketIOScope: CoroutineScope = CoroutineScope(socketIOSupervisor)
    )

    @Volatile
    private var instance: SocketInstanceHolder? = null
    private val lock = Mutex()

    override fun networkClientCleared() {
        var lockSuccess = false
        try {
            instance?.let { nnInstance ->
                lockSuccess = lock.tryLock()
                nnInstance.socket.disconnect()
                nnInstance.socketIOSupervisor.cancel()
                instance = null
                _socketIOStateFlow.value = SocketIOState.Uninitialized
            }
        } finally {
            if (lockSuccess) {
                lock.unlock()
            }
        }
    }

    init {
        networkClient.addListener(this)
    }

    override suspend fun connect(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Response<Any, ResponseError> =
        lock.withLock {
            instance?.let { nnInstance ->

                nnInstance.socket.connect()
                Response.Success(true)

            } ?: buildSocket(relayData).let { response ->

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        return response
                    }
                    is Response.Success -> {
                        instance = response.value
                        response.value.socket.connect()
                        Response.Success(true)
                    }
                }

            }
        }

    override fun disconnect() {
        instance?.socket?.disconnect()
    }

    override val isConnected: Boolean
        get() = instance?.socket?.connected() ?: false

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
    private suspend fun buildSocket(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Response<SocketInstanceHolder, ResponseError> {
        val nnRelayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl> = relayData
            ?: relayDataHandler.retrieveRelayUrlAndToken().let { response ->
                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        try {
                            response.exception?.let {
                                LOG.e(TAG, response.message, it)
                            } ?: LOG.w(TAG, response.message)
                        } catch (e: Exception) {
                            e.message?.let { LOG.e(TAG, it, e) }
                        }
                        return response
                    }
                    is Response.Success -> {
                        response.value
                    }
                }
            }

        // Need to create a new network client with timeout set to 0
        val client = networkClient.getClient().newBuilder()
            .callTimeout(0L, TimeUnit.SECONDS)
            .connectTimeout(0L, TimeUnit.SECONDS)
            .readTimeout(0L, TimeUnit.SECONDS)
            .writeTimeout(0L, TimeUnit.SECONDS)
            .build()

        val socketURI = URI(nnRelayData.third.value + "/socket.io")
        val socketURL = socketURI.toURL()

        val options: IO.Options = IO.Options().apply {
            path = socketURI.rawPath
            callFactory = client
            webSocketFactory = client
            reconnection = true

            // TODO: work out a reconnection attempt strategy to set on initialization
//            reconnectionAttempts

            timeout = 20_000L
            upgrade = true
            rememberUpgrade = false
            transports = arrayOf(Polling.NAME, WebSocket.NAME)
        }

        val socket: Socket = try {
            // TODO: Need to add listener to relayData in case it is changed
            //  need to disconnect and open a new socket.
            IO.socket(socketURL.protocol + "://" + socketURL.authority, options)
        } catch (e: Exception) {
            val msg = "Failed to create socket-io instance"
            LOG.e(TAG, msg, e)
            return Response.Error(ResponseError(msg, e))
        }

        socket.io().on(Manager.EVENT_TRANSPORT) { args ->
            try {
                (args[0] as Transport).on(Transport.EVENT_REQUEST_HEADERS) { requestArgs ->

                    val headers = requestArgs[0] as java.util.Map<String, List<String>>

                    if (nnRelayData.first.second != null) {
                        headers.put(TransportToken.TRANSPORT_TOKEN_HEADER, listOf(nnRelayData.first.second!!.value))
                    } else {
                        headers.put(AuthorizationToken.AUTHORIZATION_HEADER, listOf(nnRelayData.first.first.value))
                    }
                }
            } catch (e: Exception) {
                LOG.e(TAG, "Adding authorization to RequestHeaders failed.", e)
            }
        }

        // Client Socket Listeners
        socket.on(Socket.EVENT_CONNECT) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Connected(
                    System.currentTimeMillis()
                )
            }
            LOG.d(TAG, "CONNECT" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_CONNECTING) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Connecting
            }
            LOG.d(TAG, "CONNECTING" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_DISCONNECT) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Disconnected
            }
            LOG.d(TAG, "DISCONNECT" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_ERROR) { args ->
            LOG.e(
                TAG,
                "ERROR: ",
                try {
                    (args[0] as EngineIOException)
                        .also { _socketIOSharedFlow.tryEmit(SocketIOError.Error(it)) }
                } catch (e: Exception) {
                    // ClassCast or IndexOutOfBounds Exception
                    _socketIOSharedFlow.tryEmit(SocketIOError.Error(null))
                    null
                }
            )
        }
        socket.on(Socket.EVENT_MESSAGE) { args ->
            val argsString = args.joinToString("")
            LOG.d(TAG, "MESSAGE: $argsString")

            if (synchronizedListeners.hasListeners) {
                try {

                    val type: String = moshi.getMessageType(argsString).type

                    when (type) {
                        SphinxSocketIOMessage.Type.ChatSeen.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.ChatSeen(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseChat::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Contact.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Contact(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseContact::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Invite.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Invite(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseInvite::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.InvoicePayment.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.InvoicePayment(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseInvoice::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.Attachment.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.Attachment(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.Boost.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.Boost(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.Confirmation.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.Confirmation(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.Delete.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.Delete(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.Create.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.Create(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.Leave.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.Leave(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.Join.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.Join(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.Kick.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.Kick(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.TribeDelete.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.TribeDelete(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.Member.Request.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.Member.Request(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.Member.Approve.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.Member.Approve(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.Group.Member.Reject.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.Group.Member.Reject(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseGroup::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.KeySend.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.KeySend(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.Message.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.Message(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.Purchase.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.Purchase(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.PurchaseAccept.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.PurchaseAccept(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        SphinxSocketIOMessage.Type.MessageType.PurchaseDeny.JSON_TYPE -> {
                            SphinxSocketIOMessage.Type.MessageType.PurchaseDeny(
                                moshi.getMessageResponse(
                                    MessageResponse.ResponseMessage::class.java,
                                    argsString
                                )
                            )
                        }
                        else -> {
                            // Try to handle it as a message
                            val messageDto: MessageDto = moshi.getMessageResponse(
                                MessageResponse.ResponseMessage::class.java,
                                argsString
                            )

                            LOG.w(TAG, "SocketIO EventMessage Type '$type' not handled")

                            SphinxSocketIOMessage.Type.MessageType.Message(messageDto)
                        }
                    }.let { response ->
                        synchronizedListeners.dispatch(response)
                    }
                } catch (e: Exception) {
                    LOG.e(TAG, "SocketIO EventMessage error", e)
                }
            }

        }
        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            LOG.e(
                TAG,
                "CONNECT_ERROR: ",
                try {
                    (args[0] as EngineIOException)
                        .also { _socketIOSharedFlow.tryEmit(SocketIOError.ConnectError(it)) }
                } catch (e: Exception) {
                    _socketIOSharedFlow.tryEmit(SocketIOError.ConnectError(null))
                    // ClassCast or IndexOutOfBounds Exception
                    null
                }
            )
        }
        socket.on(Socket.EVENT_CONNECT_TIMEOUT) { args ->
            LOG.d(TAG, "CONNECT_TIMEOUT" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_RECONNECT) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Reconnected
            }
            LOG.d(TAG, "RECONNECT" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_RECONNECT_ERROR) { args ->
            LOG.e(
                TAG,
                "RECONNECT_ERROR: ",
                try {
                    (args[0] as EngineIOException)
                        .also { _socketIOSharedFlow.tryEmit(SocketIOError.ReconnectError(it)) }
                } catch (e: Exception) {
                    _socketIOSharedFlow.tryEmit(SocketIOError.ReconnectError(null))
                    // ClassCast or IndexOutOfBounds Exception
                    null
                }
            )
        }
        socket.on(Socket.EVENT_RECONNECT_FAILED) { args ->
            LOG.d(TAG, "RECONNECT_FAILED" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_RECONNECTING) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Reconnecting
            }
            LOG.d(TAG, "RECONNECTING" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_PING) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Connected(
                    System.currentTimeMillis()
                )
            }
            LOG.d(TAG, "PING" + args.joinToString(", ", ": "))
        }
        socket.on(Socket.EVENT_PONG) { args ->
            LOG.d(TAG, "PONG" + args.joinToString(", ", ": "))
        }

        // Engine Socket Listeners
        socket.io().on(EngineSocket.EVENT_OPEN) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Opened
            }
            LOG.d(TAG, "OPEN" + args.joinToString(", ", ": "))
        }
        socket.io().on(EngineSocket.EVENT_CLOSE) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Closed
            }
            LOG.d(TAG, "CLOSE" + args.joinToString(", ", ": "))
        }
        socket.io().on(EngineSocket.EVENT_UPGRADE_ERROR) { args ->
            LOG.e(
                TAG,
                "UPGRADE_ERROR: ",
                try {
                    (args[0] as EngineIOException)
                        .also { _socketIOSharedFlow.tryEmit(SocketIOError.UpgradeError(it)) }
                } catch (e: Exception) {
                    _socketIOSharedFlow.tryEmit(SocketIOError.UpgradeError(null))
                    // ClassCast or IndexOutOfBounds Exception
                    null
                }
            )
        }
//        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_FLUSH) { args ->
//            LOG.d(TAG, "FLUSH" + args.joinToString(", ", ": "))
//        }
//        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_HANDSHAKE) { args ->
//            LOG.d(TAG, "HANDSHAKE" + args.joinToString(", ", ": "))
//        }
//        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_UPGRADING) { args ->
//            LOG.d(TAG, "UPGRADING" + args.joinToString(", ", ": "))
//        }
//        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_UPGRADE) { args ->
//            LOG.d(TAG, "UPGRADE" + args.joinToString(", ", ": "))
//        }

        _socketIOStateFlow.value = SocketIOState.Initialized.Disconnected

        return Response.Success(SocketInstanceHolder(socket, client, nnRelayData))
    }
}
