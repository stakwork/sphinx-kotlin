package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.MessagePurchaseStatus

internal sealed class LayoutState {

    data class MessageStatusHeader(
        val senderName: String?,
        val showSent: Boolean,

        // TODO: rework bolt icon when sending messages to be yellow (sending), red (failed), green(sent)
        val showBoltIcon: Boolean,

        val showLockIcon: Boolean,
        val timestamp: String,
    ): LayoutState() {
        val showReceived: Boolean
            get() = !showSent
    }

    sealed class Bubble: LayoutState() {

        data class Message(val text: String): Bubble()

        data class DirectPayment(val showSent: Boolean, val amount: Sat): Bubble() {
            val showReceived: Boolean
                get() = !showSent

            val unitLabel: String
                get() = amount.unit
        }

        data class PaidMessageDetails(
            val amount: Sat,
            val purchaseStatus: MessagePurchaseStatus,
            val showPaymentAcceptedIcon: Boolean,
            val showPaymentProgressWheel: Boolean,
            val showSendPaymentIcon: Boolean,
            val showPaymentReceivedIcon: Boolean,
        ) : LayoutState() {
            val amountText: String = amount.asFormattedString(appendUnit = true)
        }

        data class PaidMessageSentStatus(
            val amount: Sat,
            val purchaseStatus: MessagePurchaseStatus,
        ) : LayoutState() {
            val amountText: String = amount.asFormattedString(appendUnit = true)
        }
    }

}
