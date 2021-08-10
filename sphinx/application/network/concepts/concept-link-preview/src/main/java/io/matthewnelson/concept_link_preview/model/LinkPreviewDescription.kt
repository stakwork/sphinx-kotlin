package io.matthewnelson.concept_link_preview.model

@Suppress("NOTHING_TO_INLINE")
inline fun String.toHtmlPreviewDescriptionOrNull(): HtmlPreviewDescription? =
    try {
        HtmlPreviewDescription(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class HtmlPreviewDescription(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "HtmlPreviewDescription cannot be empty"
        }
    }
}
