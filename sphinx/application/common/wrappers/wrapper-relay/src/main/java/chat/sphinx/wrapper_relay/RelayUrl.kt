package chat.sphinx.wrapper_relay

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRelayUrl(): RelayUrl? =
    try {
        RelayUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class RelayUrl(val value: String){
    init {
        require(value.isNotEmpty()) {
            "RelayUrl cannot be empty"
        }
    }
}
