package io.matthewnelson.concept_link_preview

import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.concept_link_preview.model.HtmlPreviewData
import io.matthewnelson.concept_link_preview.model.TribePreviewData

abstract class LinkPreviewHandler {
    abstract suspend fun retrieveHtmlPreview(url: String): HtmlPreviewData?
    abstract suspend fun retrieveTribeLinkPreview(tribeJoinLink: TribeJoinLink): TribePreviewData?
}
