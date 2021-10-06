package chat.sphinx.onboard_connect.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardConnectSubmitButtonViewState: ViewState<OnBoardConnectSubmitButtonViewState>() {
    object Enabled: OnBoardConnectSubmitButtonViewState()
    object Disabled: OnBoardConnectSubmitButtonViewState()
}