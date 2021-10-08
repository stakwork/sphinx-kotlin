package chat.sphinx.onboard_desktop.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardDesktopViewState: ViewState<OnBoardDesktopViewState>() {
    object Idle: OnBoardDesktopViewState()
}