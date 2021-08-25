package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.wrapper_common.ExternalAuthorizeLink
import chat.sphinx.wrapper_common.PeopleConnectLink
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class DeepLinkPopupViewState: ViewState<DeepLinkPopupViewState>() {

    object PopupDismissed: DeepLinkPopupViewState()

    class ExternalAuthorizePopup(
        val host: String
    ): DeepLinkPopupViewState()

    object LoadingPeopleConnectPopup: DeepLinkPopupViewState()

    object PeopleConnectEmptyMessage: DeepLinkPopupViewState()

    class PeopleConnectPopup(
        val alias: String?,
        val photoUrl: String?,
        val description: String?,
        val priceToMeet: Long?
    ): DeepLinkPopupViewState()
}