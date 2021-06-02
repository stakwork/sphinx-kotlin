package chat.sphinx.onboard_ready.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardReadyViewState: ViewState<OnBoardReadyViewState>() {
    object Idle: OnBoardReadyViewState()
    object Saving: OnBoardReadyViewState()
    object Error: OnBoardReadyViewState()
}
