package chat.sphinx.concept_socket_io

import io.socket.engine.engineio.client.EngineIOException

sealed class SocketIOState {

    object Uninitialized: SocketIOState()

    sealed class Initialized: SocketIOState() {
        object Connecting: Initialized()
        object Opened: Initialized()
        data class Connected(val lastPingTime: Long): Initialized()

        object Disconnected: Initialized()
        object Closed: Initialized()

        object Reconnecting: Initialized()
        object Reconnected: Initialized()
    }
}

sealed class SocketIOError {

    abstract val e: EngineIOException?

    data class Error(override val e: EngineIOException?): SocketIOError()
    data class ConnectError(override val e: EngineIOException?) : SocketIOError()
    data class ReconnectError(override val e: EngineIOException?) : SocketIOError()
    data class UpgradeError(override val e: EngineIOException?) : SocketIOError()
}
