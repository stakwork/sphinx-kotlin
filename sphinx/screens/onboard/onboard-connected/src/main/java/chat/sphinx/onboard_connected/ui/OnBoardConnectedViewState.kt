package chat.sphinx.onboard_connected.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardConnectedViewState: ViewState<OnBoardConnectedViewState>() {
    object Idle: OnBoardConnectedViewState()
}