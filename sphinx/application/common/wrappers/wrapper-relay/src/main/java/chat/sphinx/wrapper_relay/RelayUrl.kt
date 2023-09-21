package chat.sphinx.wrapper_relay

import kotlin.random.Random

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRelayUrl(): RelayUrl? =
    try {
        RelayUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isOnionAddress: Boolean
    get() = this
        .replaceFirst("http://", "")
        .replaceFirst("https://", "")
        .matches("([a-z2-7]{56}).onion.*".toRegex())

inline val String.withProtocol: String
    get() {
        return if (!this.contains("http")) {
            "https://$this"
        }  else {
            this
        }
    }

inline val String.isValidRelayUrl: Boolean
    get() = toRelayUrl() != null

inline val RelayUrl.isOnionAddress: Boolean
    get() = value.isOnionAddress

@JvmInline
value class RelayUrl(val value: String){
    init {
        require(value.isNotEmpty()) {
            "RelayUrl cannot be empty"
        }
    }
}
