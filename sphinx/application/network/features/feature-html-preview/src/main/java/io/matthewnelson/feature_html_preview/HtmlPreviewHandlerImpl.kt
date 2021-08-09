package io.matthewnelson.feature_html_preview

import chat.sphinx.concept_network_client.NetworkClient
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_html_preview.HtmlPreviewHandler
import io.matthewnelson.concept_html_preview.model.HtmlPreview

class HtmlPreviewHandlerImpl(
    dispatchers: CoroutineDispatchers,
    private val networkClient: NetworkClient
): HtmlPreviewHandler(), CoroutineDispatchers by dispatchers {

    companion object {
        // TODO: html preview cache
        // TODO: html preview queue
    }

    override suspend fun retrieveHtmlPreview(url: String): HtmlPreview {
        TODO("Not yet implemented")
    }
}
