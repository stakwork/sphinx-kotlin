package chat.sphinx.onboard_connect.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardConnectViewState: ViewState<OnBoardConnectViewState>() {
    object Idle: OnBoardConnectViewState()
    object NewUser: OnBoardConnectViewState()
    object ExistingUser: OnBoardConnectViewState()
}