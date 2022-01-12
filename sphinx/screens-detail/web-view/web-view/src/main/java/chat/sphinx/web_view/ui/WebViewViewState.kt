package chat.sphinx.web_view.ui

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class WebViewViewState: ViewState<WebViewViewState>() {
    object Idle: WebViewViewState()

    class FeedDataLoaded(
        val fromArticlesList: Boolean,
        val viewTitle: String,
        val url: String,
        val isFeedUrl: Boolean,
        val feedHasDestinations: Boolean,
        val ownerPhotoUrl: PhotoUrl?,
        val boostAmount: Sat?
    ): WebViewViewState()
}
