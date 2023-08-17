package chat.sphinx.video_screen.ui.viewstate

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedDestination
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_feed.FeedTitle
import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

internal sealed class SelectedVideoViewState: ViewState<SelectedVideoViewState>() {

    object Idle: SelectedVideoViewState()

    class VideoSelected(
        val id: FeedId,
        val feedId: FeedId?,
        val title: FeedTitle,
        val description: FeedDescription?,
        val url: FeedUrl,
        val localFile: File?,
        val date: DateTime?,
        val duration: FeedItemDuration?,
    ): SelectedVideoViewState()
}