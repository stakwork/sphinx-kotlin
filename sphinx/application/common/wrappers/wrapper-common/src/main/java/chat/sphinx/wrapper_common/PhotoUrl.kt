package chat.sphinx.wrapper_common

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPhotoUrl(): PhotoUrl? =
    try {
        PhotoUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class PhotoUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "PhotoUrl cannot be empty"
        }
    }
}
