package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_message.Message
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PinedMessageViewState: ViewState<PinedMessageViewState>() {
    object Idle: PinedMessageViewState()

    data class PinedMessageHeader(
        val message: Message
    ): PinedMessageViewState()
}
