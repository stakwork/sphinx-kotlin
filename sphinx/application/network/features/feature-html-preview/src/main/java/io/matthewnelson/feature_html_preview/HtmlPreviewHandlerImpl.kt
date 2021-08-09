package io.matthewnelson.feature_html_preview

import chat.sphinx.concept_network_client.NetworkClient
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_html_preview.HtmlPreviewHandler
import io.matthewnelson.concept_html_preview.model.HtmlPreview

class HtmlPreviewHandlerImpl(
    private val dispatchers: CoroutineDispatchers,
    private val networkClient: NetworkClient
) : HtmlPreviewHandler() {

    override suspend fun retrieveHtmlPreview(url: String): HtmlPreview? {
       return HtmlPreviewCache.getInstance()
           .getHtmlPreviewDataRetriever(url)
           ?.getHtmlPreview(dispatchers, networkClient.getClient())
    }

}
