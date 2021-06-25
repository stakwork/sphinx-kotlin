package chat.sphinx.wrapper_message_media.token

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaMUIDOrNull(): MediaMUID? =
    try {
        MediaMUID(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MediaMUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MediaMUID cannot be empty"
        }
    }
}
