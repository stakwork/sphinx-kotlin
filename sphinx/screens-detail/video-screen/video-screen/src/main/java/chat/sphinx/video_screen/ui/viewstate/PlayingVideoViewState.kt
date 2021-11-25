package chat.sphinx.video_screen.ui.viewstate

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedTitle
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PlayingVideoViewState: ViewState<PlayingVideoViewState>() {

    object Idle: PlayingVideoViewState()

    class PlayingVideo(
        val id: FeedId,
        val title: FeedTitle,
        val description: FeedDescription?,
        val url: FeedUrl,
        val date: DateTime?,
    ): PlayingVideoViewState()
}