package chat.sphinx.concept_socket_io

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

    abstract val e: Exception?

    data class Error(override val e: Exception?): SocketIOError()
    data class ConnectError(override val e: Exception?): SocketIOError()
    data class ReconnectError(override val e: Exception?): SocketIOError()
    data class UpgradeError(override val e: Exception?): SocketIOError()
}
