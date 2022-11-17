package chat.sphinx.common_player.viewstate

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedTitle
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CommonPlayerScreenViewState: ViewState<CommonPlayerScreenViewState>() {

    object Idle: CommonPlayerScreenViewState()

    class FeedLoaded(
        val title: FeedTitle,
        val imageToShow: PhotoUrl?,
        val chatId: ChatId?,
        val subscribed: Subscribed,
        val items: List<FeedItem>,
        val hasDestinations: Boolean
    ): CommonPlayerScreenViewState()
}
