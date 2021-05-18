package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.unit

sealed class LayoutState {

    data class MessageStatusHeader(
        val senderName: String?,
        val isOutGoingMessage: Boolean,
        val showBoltIcon: Boolean,
        val showLockIcon: Boolean,
        val timestamp: String,
    ) {
        val isIncomingMessage: Boolean
            get() = !isOutGoingMessage
    }

    data class MessageTypeMessageContent(
        val messageContent: String
    ): LayoutState()

    data class DirectPayment(
        val showSent: Boolean,
        val amount: Sat
    ): LayoutState() {
        val showReceived: Boolean
            get() = !showSent

        val unitLabel: String
            get() = amount.unit
    }

}
