package chat.sphinx.wrapper_relay

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRelayHMacKey(): RelayHMacKey? =
    try {
        RelayHMacKey(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class RelayHMacKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "RelayHMacKey cannot be empty"
        }
    }
}
