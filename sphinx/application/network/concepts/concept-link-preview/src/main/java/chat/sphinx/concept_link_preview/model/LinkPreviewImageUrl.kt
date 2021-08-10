package chat.sphinx.concept_link_preview.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPreviewImageUrlOrNull(): PreviewImageUrl? =
    try {
        PreviewImageUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class PreviewImageUrl(val value: String) {
    init {
        require(value.toHttpUrlOrNull() != null) {
            "HtmlPreviewDescription was not a valid url"
        }
    }
}
