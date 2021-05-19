package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.MessagePurchaseStatus
import chat.sphinx.wrapper_message.isAccepted
import chat.sphinx.wrapper_message.isProcessing
import chat.sphinx.wrapper_message.labelText

internal sealed class LayoutState {

    data class MessageStatusHeader(
        val senderName: String?,
        val isOutGoingMessage: Boolean,

        // TODO: rework bolt icon when sending messages to be yellow (sending), red (failed), green(sent)
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

    data class PaidMessageDetailsContent(
        val isIncoming: Boolean,
        val amount: Sat,
        val purchaseStatus: MessagePurchaseStatus = MessagePurchaseStatus.NoStatusMessage,
    ): LayoutState() {
        val showSentMessageStatusHeader: Boolean
            get() = !isIncoming

        val amountText: String = amount.asFormattedString(appendUnit = true)

        val paymentStatusText: String
            get() = purchaseStatus.labelText

        val isPaymentProcessing: Boolean
            get() = purchaseStatus.isProcessing

        val isPaymentAccepted: Boolean
            get() = purchaseStatus.isAccepted
    }
}
