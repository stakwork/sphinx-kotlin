package chat.sphinx.onboard_picture.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardPictureViewState: ViewState<OnBoardPictureViewState>() {
    object Idle: OnBoardPictureViewState()
}
