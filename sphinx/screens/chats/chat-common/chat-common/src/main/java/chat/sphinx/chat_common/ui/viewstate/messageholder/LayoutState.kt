package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageContentDecrypted

sealed class LayoutState {

    data class MessageTypeMessageContent(
        val messageContent: String
    ): LayoutState()

    data class DirectPayment(
        val showSent: Boolean,
        val amount: Sat
    ): LayoutState() {
        val showReceived: Boolean = !showSent
        val unitLabel: String = amount.unit
    }

}
