package chat.sphinx.video_screen.ui.detail

import io.matthewnelson.concept_views.viewstate.ViewState

@Suppress("ClassName")
internal sealed class VideoFeedDetailScreenViewState: ViewState<VideoFeedDetailScreenViewState>() {
    object Idle: VideoFeedDetailScreenViewState()
}