package chat.sphinx.concept_link_preview.model

import chat.sphinx.wrapper_common.PhotoUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPreviewImageUrlOrNull(): PreviewImageUrl? =
    try {
        PreviewImageUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun PreviewImageUrl.toPhotoUrl(): PhotoUrl =
    PhotoUrl(value)

@Suppress("NOTHING_TO_INLINE")
inline fun PhotoUrl.toPreviewImageUrlOrNull(): PreviewImageUrl? =
    value.toPreviewImageUrlOrNull()

@JvmInline
value class PreviewImageUrl(val value: String) {
    init {
        require(value.toHttpUrlOrNull() != null) {
            "PreviewImageUrl was not a valid url"
        }
    }
}
