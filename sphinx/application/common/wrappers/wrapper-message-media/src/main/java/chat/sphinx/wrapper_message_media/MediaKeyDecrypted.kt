package chat.sphinx.wrapper_message_media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaKeyDecrypted(): MediaKeyDecrypted? =
    MediaKeyDecrypted(this)

@JvmInline
value class MediaKeyDecrypted(val value: String)
