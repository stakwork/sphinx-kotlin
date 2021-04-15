package chat.sphinx.wrapper_message.media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaToken(): MediaToken? =
    try {
        MediaToken(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class MediaToken(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MediaToken cannot be empty"
        }
    }
}
