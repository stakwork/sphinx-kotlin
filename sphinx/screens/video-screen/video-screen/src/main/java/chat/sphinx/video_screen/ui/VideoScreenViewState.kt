package chat.sphinx.video_screen.ui

import io.matthewnelson.concept_views.viewstate.ViewState

@Suppress("ClassName")
internal sealed class VideoScreenViewState: ViewState<VideoScreenViewState>() {
    object Idle: VideoScreenViewState()
}