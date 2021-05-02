package chat.sphinx.concept_socket_io

import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import io.socket.client.client.Socket
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class SocketIOManager {
    abstract suspend fun getSocket(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Response<Socket, ResponseError>

    abstract val socketIOStateFlow: StateFlow<SocketIOState>
    abstract val socketIOErrorFlow: SharedFlow<SocketIOError>
}
