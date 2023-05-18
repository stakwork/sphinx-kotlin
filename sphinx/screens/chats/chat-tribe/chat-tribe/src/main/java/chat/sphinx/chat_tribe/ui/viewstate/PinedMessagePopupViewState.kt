package chat.sphinx.chat_tribe.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PinedMessagePopupViewState: ViewState<PinedMessagePopupViewState>() {

    object Idle: PinedMessagePopupViewState()

    data class Visible(
        val text: String
    ): PinedMessagePopupViewState()
}
