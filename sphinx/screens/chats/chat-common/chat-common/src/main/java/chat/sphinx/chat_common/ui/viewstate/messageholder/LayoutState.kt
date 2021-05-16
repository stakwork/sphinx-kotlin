package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.Message

sealed class LayoutState {

    data class MessageTypeContent(
        val message: Message,
    ): LayoutState() {
        val messageText: String? = message.messageContentDecrypted?.value
    }

    data class DirectPayment(
        val showSent: Boolean,
        val amount: Sat
    ): LayoutState() {
        val showReceived: Boolean = !showSent
        val unitLabel: String = amount.unit
    }

}
