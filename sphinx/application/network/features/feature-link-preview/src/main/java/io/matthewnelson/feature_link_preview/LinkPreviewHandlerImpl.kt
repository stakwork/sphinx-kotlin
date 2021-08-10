package io.matthewnelson.feature_link_preview

import chat.sphinx.concept_network_client.NetworkClient
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_link_preview.LinkPreviewHandler
import io.matthewnelson.concept_link_preview.model.LinkPreviewData

class LinkPreviewHandlerImpl(
    private val dispatchers: CoroutineDispatchers,
    private val networkClient: NetworkClient
) : LinkPreviewHandler() {

    override suspend fun retrieveHtmlPreview(url: String): LinkPreviewData? {
       return LinkPreviewCache.getInstance()
           .getHtmlPreviewDataRetriever(url)
           ?.getHtmlPreview(dispatchers, networkClient.getClient())
    }

}
