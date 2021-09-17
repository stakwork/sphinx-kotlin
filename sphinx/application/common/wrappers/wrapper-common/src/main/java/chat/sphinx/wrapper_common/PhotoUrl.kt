package chat.sphinx.wrapper_common

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPhotoUrl(): PhotoUrl? =
    try {
        PhotoUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline val PhotoUrl.thumbnailUrl: PhotoUrl
    get() = PhotoUrl(this.value + "?thumb=true")

@JvmInline
value class PhotoUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "PhotoUrl cannot be empty"
        }
    }
}
