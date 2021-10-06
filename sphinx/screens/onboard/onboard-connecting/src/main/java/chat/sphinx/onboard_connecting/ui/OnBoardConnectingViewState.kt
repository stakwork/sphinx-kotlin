package chat.sphinx.onboard_connecting.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardConnectingViewState: ViewState<OnBoardConnectingViewState>() {
    object Idle: OnBoardConnectingViewState()
}