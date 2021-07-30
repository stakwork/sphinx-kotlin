package chat.sphinx.wrapper_relay

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRelayUrl(): RelayUrl? =
    try {
        RelayUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val RelayUrl.isOnionAddress: Boolean
    get() = value
        .replaceFirst("http://", "")
        .replaceFirst("https://", "")
        .matches("([a-z2-7]{56}).onion.*".toRegex())

@JvmInline
value class RelayUrl(val value: String){
    init {
        require(value.isNotEmpty()) {
            "RelayUrl cannot be empty"
        }
    }
}
