package io.matthewnelson.concept_link_preview

import io.matthewnelson.concept_link_preview.model.HtmlPreviewData

abstract class LinkPreviewHandler {
    abstract suspend fun retrieveHtmlPreview(url: String): HtmlPreviewData?
}
