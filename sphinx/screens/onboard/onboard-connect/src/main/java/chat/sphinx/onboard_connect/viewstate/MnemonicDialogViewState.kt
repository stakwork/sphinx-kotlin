package chat.sphinx.onboard_connect.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class MnemonicDialogViewState: ViewState<MnemonicDialogViewState>() {
    object Idle: MnemonicDialogViewState()
    object Loading: MnemonicDialogViewState()
}