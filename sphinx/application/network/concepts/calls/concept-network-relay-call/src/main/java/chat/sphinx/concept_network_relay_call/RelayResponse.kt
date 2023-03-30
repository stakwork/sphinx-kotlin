package chat.sphinx.concept_network_relay_call

/**
 * A wrapper for Relay specific responses. This should *never* be exposed
 * to concept modules, as [T] should be what is returned.
 * */
abstract class RelayResponse<T: Any> {
    abstract val success: Boolean
    abstract val response: T?
    abstract val error: String?
}

abstract class RelayListResponse<T: Any> {
    abstract val success: Boolean
    abstract val response: List<T>?
    abstract val error: String?
}
