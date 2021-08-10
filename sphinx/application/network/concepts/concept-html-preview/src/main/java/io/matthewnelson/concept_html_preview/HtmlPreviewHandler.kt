package io.matthewnelson.concept_html_preview

import io.matthewnelson.concept_html_preview.model.HtmlPreview

abstract class HtmlPreviewHandler {
    abstract suspend fun retrieveHtmlPreview(url: String): HtmlPreview?
}
