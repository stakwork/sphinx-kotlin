package chat.sphinx.video_screen.ui.viewstate

import android.net.Uri
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.FeedItemDuration
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class VideoPlayerViewState: ViewState<VideoPlayerViewState>() {

    object Idle: VideoPlayerViewState()

    class YoutubeVideoIframe(val videoId: FeedId): VideoPlayerViewState()
    class WebViewPlayer(val videoUri: Uri, val duration: FeedItemDuration?): VideoPlayerViewState()

}