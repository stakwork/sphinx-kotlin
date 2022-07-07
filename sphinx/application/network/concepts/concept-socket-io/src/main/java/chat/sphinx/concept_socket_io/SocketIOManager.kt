package chat.sphinx.concept_socket_io

import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class SocketIOManager {
    abstract suspend fun connect(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Response<Any, ResponseError>

    abstract val isConnected: Boolean

    abstract fun disconnect()

    abstract val socketIOStateFlow: StateFlow<SocketIOState>
    abstract val socketIOErrorFlow: SharedFlow<SocketIOError>

    abstract fun addListener(listener: SphinxSocketIOMessageListener): Boolean
    abstract fun removeListener(listener: SphinxSocketIOMessageListener): Boolean
}
