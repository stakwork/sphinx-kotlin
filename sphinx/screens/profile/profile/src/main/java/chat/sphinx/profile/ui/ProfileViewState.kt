package chat.sphinx.profile.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ProfileViewState: ViewState<ProfileViewState>() {
    object Basic: ProfileViewState()
    data class Advanced(val deviceSetupButtonTitle: String): ProfileViewState()
}
