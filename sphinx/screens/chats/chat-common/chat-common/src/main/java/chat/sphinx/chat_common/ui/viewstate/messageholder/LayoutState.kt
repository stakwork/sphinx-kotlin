package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.MessageType

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

    data class DeletedMessage(
        val gravityStart: Boolean,
        val timestamp: String,
    ): LayoutState()

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
            val purchaseType: MessageType.Purchase?,
            val bubbleBackground: BubbleBackground,
            val isShowingReceivedMessage: Boolean,
            val showPaymentAcceptedIcon: Boolean,
            val showPaymentProgressWheel: Boolean,
            val showSendPaymentIcon: Boolean,
            val showPaymentReceivedIcon: Boolean,
        ): LayoutState() {
            val amountText: String
                get() = amount.asFormattedString(appendUnit = true)
        }

        data class PaidMessageSentStatus(
            val amount: Sat,
            val purchaseType: MessageType.Purchase?,
        ): LayoutState() {
            val amountText: String
                get() = amount.asFormattedString(appendUnit = true)
        }
    }
}
