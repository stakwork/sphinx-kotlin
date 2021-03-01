package chat.sphinx.onboard.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardViewState: ViewState<OnBoardViewState>() {
    object Idle: OnBoardViewState()
    object DecryptKeys: OnBoardViewState()
}