package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_message.*

internal inline val MessageHolderViewState.isReceived: Boolean
    get() = this is MessageHolderViewState.Received

internal inline val MessageHolderViewState.showReceivedBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Received

internal val MessageHolderViewState.showSentBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Sent

internal sealed class MessageHolderViewState(
    val message: Message,
    chatType: ChatType?,
    val background: BubbleBackground,
    val initialHolder: InitialHolderViewState
) {

    val statusHeader: LayoutState.MessageStatusHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (background is BubbleBackground.First) {
            LayoutState.MessageStatusHeader(
                if (chatType?.isConversation() != false) null else message.senderAlias?.value,
                this is Sent,
                this is Sent && (message.status.isReceived() || message.status.isConfirmed()),
                message.messageContentDecrypted != null,
                DateTime.getFormathmma().format(message.date.value),
            )
        } else {
            null
        }
    }

    val bubbleDirectPayment: LayoutState.Bubble.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.type.isDirectPayment()) {
            LayoutState.Bubble.DirectPayment(showSent = this is Sent, amount = message.amount)
        } else {
            null
        }
    }

    val bubbleMessage: LayoutState.Bubble.Message? by lazy(LazyThreadSafetyMode.NONE) {
        message.messageContentDecrypted?.let {
            // TODO: Handle podcast clips
            message.giphyData?.let { giphyData ->
                // TODO: show only the giphyData.text when rendering logic is implemented
//                giphyData.text?.let { text ->
//                    LayoutState.Bubble.Message(text = text)
//                }
                LayoutState.Bubble.Message(text = giphyData.toString())
            } ?: /*if (message.podBoost == null) {*/ // TODO: Uncomment once boost layout logic is implemented
            LayoutState.Bubble.Message(text = it.value)
//            } else {
//                null
//            }
        }
    }

    val bubblePaidMessageDetails: LayoutState.Bubble.PaidMessageDetails? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage) {
            null
        } else {
            val isPaymentPending = message.status.isPending()

            message.type.let { type ->
                LayoutState.Bubble.PaidMessageDetails(
                    amount = message.amount,
                    purchaseType = if (type.isPurchase()) type else null,
                    showPaymentAcceptedIcon = type.isPurchaseAccepted(),
                    showPaymentProgressWheel = type.isPurchaseProcessing(),
                    showSendPaymentIcon = this !is Sent && !isPaymentPending,
                    showPaymentReceivedIcon = this is Sent && !isPaymentPending,
                )
            }
        }
    }

    val bubblePaidMessageSentStatus: LayoutState.Bubble.PaidMessageSentStatus? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage || this !is Sent) {
            null
        } else {
            message.type.let { type ->
                LayoutState.Bubble.PaidMessageSentStatus(
                    amount = message.amount,
                    purchaseType = if (type.isPurchase()) type else null,
                )
            }
        }
    }


    class Sent(
        message: Message,
        chatType: ChatType?,
        background: BubbleBackground,
    ): MessageHolderViewState(
        message,
        chatType,
        background,
        InitialHolderViewState.None
    )

    class Received(
        message: Message,
        chatType: ChatType?,
        background: BubbleBackground,
        initialHolder: InitialHolderViewState,
    ): MessageHolderViewState(
        message,
        chatType,
        background,
        initialHolder
    )
}
