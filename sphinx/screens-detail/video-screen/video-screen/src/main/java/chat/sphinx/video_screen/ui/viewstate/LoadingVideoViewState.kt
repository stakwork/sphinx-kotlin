package chat.sphinx.video_screen.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class LoadingVideoViewState: ViewState<LoadingVideoViewState>() {
    object Idle: LoadingVideoViewState()
    object MetaDataLoaded: LoadingVideoViewState()
}
