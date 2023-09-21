package chat.sphinx.profile.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SignerViewState: ViewState<SignerViewState>() {
    object Idle: SignerViewState()
    object HardwareDevice: SignerViewState()
    object PhoneDevice: SignerViewState()
}
