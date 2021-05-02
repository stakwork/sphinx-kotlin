package chat.sphinx.feature_socket_io

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.retrieveRelayUrlAndAuthorizationToken
import chat.sphinx.concept_socket_io.SocketIOError
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SocketIOState
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.logger.w
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import io.socket.client.client.IO
import io.socket.client.client.Manager
import io.socket.client.client.Socket
import io.socket.engine.engineio.client.EngineIOException
import io.socket.engine.engineio.client.Transport
import io.socket.engine.engineio.client.transports.Polling
import io.socket.engine.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SocketIOManagerImpl(
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler,
    private val LOG: SphinxLogger,
): SocketIOManager() {

    companion object {
        const val TAG = "SocketIOManagerImpl"
    }

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


    private class SocketClientHolder(val socket: Socket, val socketIOClient: OkHttpClient)

    @Volatile
    private var instance: SocketClientHolder? = null
    private val lock = Mutex()

    override suspend fun getSocket(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Response<Socket, ResponseError> =
        instance?.let { Response.Success(it.socket) } ?: lock.withLock {
            instance?.let { Response.Success(it.socket) }
                ?: buildSocket(relayData).let { response ->
                    @Exhaustive
                    when (response) {
                        is Response.Error -> {
                            return response
                        }
                        is Response.Success -> {
                            instance = response.value
                            return Response.Success(response.value.socket)
                        }
                    }
                }
        }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
    private suspend fun buildSocket(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Response<SocketClientHolder, ResponseError> {
        val nnRelayData: Pair<AuthorizationToken, RelayUrl> = relayData
            ?: relayDataHandler.retrieveRelayUrlAndAuthorizationToken().let { response ->
                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        response.exception?.let {
                            LOG.e(TAG, response.message, it)
                        } ?: LOG.w(TAG, response.message)
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

        val options: IO.Options = IO.Options().apply {
            callFactory = client
            webSocketFactory = client
            reconnection = true
            timeout = 20_000L
            upgrade = true
            rememberUpgrade = false
            transports = arrayOf(Polling.NAME, WebSocket.NAME)
        }

        val socket: Socket = try {
            IO.socket(nnRelayData.second.value, options)
        } catch (e: Exception) {
            val msg = "Failed to create socket-io instance"
            LOG.e(TAG, msg, e)
            return Response.Error(ResponseError(msg, e))
        }

        socket.io().on(Manager.EVENT_TRANSPORT) { args ->
            try {
                (args[0] as Transport).on(Transport.EVENT_REQUEST_HEADERS) { requestArgs ->

                    val headers = requestArgs[0] as java.util.Map<String, List<String>>
                    // TODO: Update string value to use constant from AuthorizationToken class
                    //  once PR#66 is merged.
                    headers.put("X-User-Token", listOf(nnRelayData.first.value))

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
            LOG.d(TAG, "MESSAGE" + args.joinToString(", ", ": "))
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
        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_OPEN) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Opened
            }
            LOG.d(TAG, "OPEN" + args.joinToString(", ", ": "))
        }
        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_CLOSE) { args ->
            if (_socketIOStateFlow.value != SocketIOState.Uninitialized) {
                _socketIOStateFlow.value = SocketIOState.Initialized.Closed
            }
            LOG.d(TAG, "CLOSE" + args.joinToString(", ", ": "))
        }
        socket.io().on(io.socket.engine.engineio.client.Socket.EVENT_UPGRADE_ERROR) { args ->
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

        return Response.Success(SocketClientHolder(socket, client))
    }
}
