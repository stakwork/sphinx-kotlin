package io.matthewnelson.concept_html_preview.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Suppress("NOTHING_TO_INLINE")
inline fun String.toHtmlPreviewImageUrlOrNull(): HtmlPreviewImageUrl? =
    try {
        HtmlPreviewImageUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class HtmlPreviewImageUrl(val value: String) {
    init {
        require(value.toHttpUrlOrNull() != null) {
            "HtmlPreviewDescription was not a valid url"
        }
    }
}
