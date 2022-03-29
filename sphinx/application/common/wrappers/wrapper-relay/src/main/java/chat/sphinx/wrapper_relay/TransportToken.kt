package chat.sphinx.wrapper_relay

@Suppress("NOTHING_TO_INLINE")
inline fun String.toTransportToken(): TransportToken? =
    try {
        TransportToken(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class TransportToken(val value: String) {

    companion object {
        const val TRANSPORT_TOKEN_HEADER = "x-transport-token"
    }

    init {
        require(value.isNotEmpty()) {
            "TransportToken cannot be empty"
        }
    }
}
