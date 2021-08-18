package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.wrapper_common.ExternalAuthorizeLink
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class DeepLinkPopupViewState: ViewState<DeepLinkPopupViewState>() {

    object PopupDismissed: DeepLinkPopupViewState()

    class ExternalAuthorizePopup(
        val link: ExternalAuthorizeLink
    ): DeepLinkPopupViewState()
}