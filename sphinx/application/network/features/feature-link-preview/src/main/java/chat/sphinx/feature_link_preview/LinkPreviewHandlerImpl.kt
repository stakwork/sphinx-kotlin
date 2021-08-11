package chat.sphinx.feature_link_preview

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import chat.sphinx.concept_link_preview.LinkPreviewHandler
import chat.sphinx.concept_link_preview.model.HtmlPreviewData
import chat.sphinx.concept_link_preview.model.TribePreviewData

class LinkPreviewHandlerImpl(
    private val dispatchers: CoroutineDispatchers,
    private val networkClient: NetworkClient,
    private val networkQueryChat: NetworkQueryChat,
) : LinkPreviewHandler() {

    override suspend fun retrieveHtmlPreview(url: String): HtmlPreviewData? {
       return LinkPreviewCache.getInstance()
           .getHtmlPreviewDataRetriever(url)
           ?.getHtmlPreview(dispatchers, networkClient.getClient())
    }

    override suspend fun retrieveTribeLinkPreview(tribeJoinLink: TribeJoinLink): TribePreviewData? {
        return LinkPreviewCache.getInstance()
            .getTribePreviewDataRetriever(tribeJoinLink)
            ?.getTribePreview(networkQueryChat)
    }
}
