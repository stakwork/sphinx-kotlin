package chat.sphinx.onboard_name.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardNameViewState: ViewState<OnBoardNameViewState>() {
    object Idle: OnBoardNameViewState()
    object Saving: OnBoardNameViewState()
    object Error: OnBoardNameViewState()
}
