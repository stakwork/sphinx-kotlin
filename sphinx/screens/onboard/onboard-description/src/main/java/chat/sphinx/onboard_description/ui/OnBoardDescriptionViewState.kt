package chat.sphinx.onboard_description.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardDescriptionViewState: ViewState<OnBoardDescriptionViewState>() {
    object Idle: OnBoardDescriptionViewState()
    object NewUser: OnBoardDescriptionViewState()
    object ExistingUser: OnBoardDescriptionViewState()
}