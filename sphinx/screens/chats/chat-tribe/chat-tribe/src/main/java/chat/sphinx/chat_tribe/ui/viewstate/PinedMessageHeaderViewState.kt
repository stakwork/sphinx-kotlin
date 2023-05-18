package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_message.Message
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PinedMessageHeaderViewState: ViewState<PinedMessageHeaderViewState>() {
    object Idle: PinedMessageHeaderViewState()

    data class PinedMessageHeader(
        val message: Message
    ): PinedMessageHeaderViewState()


}
