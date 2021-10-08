package chat.sphinx.onboard_lightning.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardLightningViewState: ViewState<OnBoardLightningViewState>() {
    object Idle: OnBoardLightningViewState()
}