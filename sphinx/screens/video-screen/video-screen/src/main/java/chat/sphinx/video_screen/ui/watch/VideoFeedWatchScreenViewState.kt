package chat.sphinx.video_screen.ui.watch

import io.matthewnelson.concept_views.viewstate.ViewState

@Suppress("ClassName")
internal sealed class VideoFeedWatchScreenViewState: ViewState<VideoFeedWatchScreenViewState>() {
    object Idle: VideoFeedWatchScreenViewState()
}