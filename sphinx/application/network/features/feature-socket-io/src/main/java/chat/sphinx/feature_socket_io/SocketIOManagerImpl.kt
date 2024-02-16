package chat.sphinx.feature_socket_io

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_client.NetworkClientClearedListener
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.retrieveRelayUrlAndToken
import chat.sphinx.concept_socket_io.*
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
    @Suppress("RemoveExplicitTypeArguments")
    private val _socketIOSharedFlow: MutableSharedFlow<SocketIOError> by lazy {
        MutableSharedFlow<SocketIOError>(0, 1)
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

}
