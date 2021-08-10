package chat.sphinx.concept_link_preview.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Suppress("NOTHING_TO_INLINE")
inline fun String.toHtmlPreviewFavIconUrlOrNull(): HtmlPreviewFavIconUrl? =
    try {
        HtmlPreviewFavIconUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class HtmlPreviewFavIconUrl(val value: String) {
    init {
        require(value.toHttpUrlOrNull() != null) {
            "HtmlPreviewFavIconUrl was not a valid url"
        }
    }
}
