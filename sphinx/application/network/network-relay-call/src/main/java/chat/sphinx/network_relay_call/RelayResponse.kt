package chat.sphinx.network_relay_call

abstract class RelayResponse<T: Any> {
    abstract val success: Boolean
    abstract val response: T?
    abstract val error: String?
}
