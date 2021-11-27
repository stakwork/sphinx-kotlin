package chat.sphinx.newsletter_detail.ui

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedTitle
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class NewsletterDetailViewState: ViewState<NewsletterDetailViewState>() {
    object Idle: NewsletterDetailViewState()

    class FeedLoaded(
        val image: PhotoUrl?,
        val title: FeedTitle,
        val description: FeedDescription?,
        val items: List<FeedItem>
    ): NewsletterDetailViewState()
}
