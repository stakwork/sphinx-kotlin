package chat.sphinx.wrapper_message_media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaKey(): MediaKey? =
    MediaKey(this)

@JvmInline
value class MediaKey(val value: String)
