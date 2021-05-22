package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.*

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
        val messageContent: String,
        val hasExtraTopPadding: Boolean = false
    ) : LayoutState()

    data class DirectPayment(
        val showSent: Boolean,
        val amount: Sat
    ) : LayoutState() {
        val showReceived: Boolean
            get() = !showSent

        val unitLabel: String
            get() = amount.unit
    }

    data class PaidMessageDetailsContent(
        val amount: Sat,
        val paymentStatusText: String,
        val showPaidMessageReceivedDetails: Bool,
//        val purchaseStatus: MessagePurchaseStatus = MessagePurchaseStatus.NoStatusMessage,
        val isPaymentProcessing: Boolean,
        val isPaymentAccepted: Boolean,
        val showSendPaymentIcon: Boolean,
        val showPaymentReceivedIcon: Boolean,
    ) : LayoutState() {
        val showSentMessageStatusHeader: Boolean
            get() = !showPaidMessageReceivedDetails

        val amountText: String = amount.asFormattedString(appendUnit = true)

//        val paymentStatusText: String
//            get() = if (isIncoming) purchaseStatus.incomingLabelText else purchaseStatus.outgoingLabelText

//        val isPaymentProcessing: Boolean
//            get() = purchaseStatus.isProcessing
//
//        val isPaymentAccepted: Boolean
//            get() = purchaseStatus.isAccepted
//
//        val showSendPaymentIcon: Boolean
//            get() = isIncoming && !isPaymentProcessing
//
//        val showPaymentReceivedIcon: Boolean
//            get() = !isIncoming && !isPaymentProcessing
    }
}
