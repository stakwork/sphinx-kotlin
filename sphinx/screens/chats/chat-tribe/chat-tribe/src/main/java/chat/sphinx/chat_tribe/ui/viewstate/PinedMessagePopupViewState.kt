package chat.sphinx.chat_tribe.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PinedMessagePopupViewState: ViewState<PinedMessagePopupViewState>() {

    object Idle: PinedMessagePopupViewState()

    data class PinnedMessage(
        val text: String
    ): PinedMessagePopupViewState()

    data class UnpinnedMessage(
        val text: String
    ): PinedMessagePopupViewState()
}
