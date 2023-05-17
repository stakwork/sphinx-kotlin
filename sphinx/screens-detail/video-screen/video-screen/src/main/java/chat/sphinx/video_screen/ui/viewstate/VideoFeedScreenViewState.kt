package chat.sphinx.video_screen.ui.viewstate

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedTitle
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class VideoFeedScreenViewState: ViewState<VideoFeedScreenViewState>() {

    object Idle: VideoFeedScreenViewState()

    class FeedLoaded(
        val title: FeedTitle,
        val imageToShow: PhotoUrl?,
        val chatId: ChatId?,
        val subscribed: Subscribed,
        val items: List<FeedItem>,
        val hasDestinations: Boolean,
        val satsPerMinute: Sat?
    ): VideoFeedScreenViewState()
}