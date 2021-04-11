package chat.sphinx.wrapper_message.media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaType(): MediaType? =
    try {
        MediaType(this)
    } catch (e: IllegalArgumentException) {
        null
    }

// TODO: Build out extensions for different types

inline class MediaType(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MediaType cannot be empty"
        }
    }
}
