package chat.sphinx.wrapper_message_media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaKeyDecrypted(): MediaKeyDecrypted? =
    try {
        MediaKeyDecrypted(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class MediaKeyDecrypted(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "MediaKeyDecrypted cannot be empty"
        }
    }
}

