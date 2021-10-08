package chat.sphinx.onboard.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardMessageViewState: ViewState<OnBoardMessageViewState>() {
    object Idle: OnBoardMessageViewState()
    object Saving: OnBoardMessageViewState()
    object Error: OnBoardMessageViewState()
}
