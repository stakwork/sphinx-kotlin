package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit

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
        val amount: Sat
    ): LayoutState() {
        private val unitLabel: String = amount.unit

        val amountText: String = "${amount.asFormattedString()} $unitLabel"

        // TODO: Switch on the current payment processing state
        val paymentStatusText: String
            get() {
                return "Pay"
            }

        // TODO: Switch on the resulting state of a completed payment
        val isPaymentCompletedSuccessfully: Boolean
            get() {
                return false
            }


        val isPaymentProcessing: Boolean = false
    }
}
