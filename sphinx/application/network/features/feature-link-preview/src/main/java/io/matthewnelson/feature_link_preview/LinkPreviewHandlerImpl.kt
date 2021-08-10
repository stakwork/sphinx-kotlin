package io.matthewnelson.feature_link_preview

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_link_preview.LinkPreviewHandler
import io.matthewnelson.concept_link_preview.model.HtmlPreviewData
import io.matthewnelson.concept_link_preview.model.TribePreviewData

class LinkPreviewHandlerImpl(
    private val dispatchers: CoroutineDispatchers,
    private val networkClient: NetworkClient
) : LinkPreviewHandler() {

    override suspend fun retrieveHtmlPreview(url: String): HtmlPreviewData? {
       return LinkPreviewCache.getInstance()
           .getHtmlPreviewDataRetriever(url)
           ?.getHtmlPreview(dispatchers, networkClient.getClient())
    }

    override suspend fun retrieveTribeLinkPreview(tribeJoinLink: TribeJoinLink): TribePreviewData? {
        return LinkPreviewCache.getInstance()
            .getTribePreviewDataRetriever(tribeJoinLink)
            .getTribePreview(dispatchers)
    }
}
