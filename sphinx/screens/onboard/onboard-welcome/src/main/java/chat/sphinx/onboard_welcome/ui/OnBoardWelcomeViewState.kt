package chat.sphinx.onboard_welcome.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardWelcomeViewState: ViewState<OnBoardWelcomeViewState>() {
    object Idle: OnBoardWelcomeViewState()
}
