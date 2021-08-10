package io.matthewnelson.concept_link_preview

import io.matthewnelson.concept_link_preview.model.LinkPreviewData

abstract class LinkPreviewHandler {
    abstract suspend fun retrieveHtmlPreview(url: String): LinkPreviewData?
}
