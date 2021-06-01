package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_message.*

internal inline val MessageHolderViewState.isReceived: Boolean
    get() = this is MessageHolderViewState.Received

internal inline val MessageHolderViewState.showReceivedBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Received

internal val MessageHolderViewState.showSentBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Sent

fun main() {
    val set: MutableSet<String> = LinkedHashSet(3)
    println(set.size)
    set.add("new string")
    println(set.size)
}

internal sealed class MessageHolderViewState(
    val message: Message,
    chat: Chat,
    val background: BubbleBackground,
    val initialHolder: InitialHolderViewState,
    val messageSenderName: (Message) -> String,
) {

    companion object {
        val unsupportedMessageTypes: List<MessageType> by lazy {
            listOf(
                MessageType.Attachment,
                MessageType.BotRes,
                MessageType.Invoice,
                MessageType.Payment,
                MessageType.TribeDelete,
            )
        }
    }

    val unsupportedMessageType: LayoutState.UnsupportedMessageType? by lazy(LazyThreadSafetyMode.NONE) {
        if (unsupportedMessageTypes.contains(message.type)) {
            LayoutState.UnsupportedMessageType(
                messageType = message.type,
                gravityStart = this is Received,
            )
        } else {
            null
        }
    }

    val statusHeader: LayoutState.MessageStatusHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (background is BubbleBackground.First) {
            LayoutState.MessageStatusHeader(
                if (chat.type.isConversation()) null else message.senderAlias?.value,
                this is Sent,
                this is Sent && (message.status.isReceived() || message.status.isConfirmed()),
                message.messageContentDecrypted != null,
                DateTime.getFormathmma().format(message.date.value),
            )
        } else {
            null
        }
    }

    val deletedMessage: LayoutState.DeletedMessage? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.status.isDeleted()) {
            LayoutState.DeletedMessage(
                gravityStart = this is Received,
                timestamp = DateTime.getFormathmma().format(message.date.value)
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
        message.retrieveTextToShow()?.let { text ->
            LayoutState.Bubble.Message(text = text)
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
                    isShowingReceivedMessage = this is Received,
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

    // don't use by lazy as this uses a for loop and needs to be initialized on a background
    // thread (so, while the MHVS is being created)
    val bubbleReactionBoosts: LayoutState.Bubble.ContainerBottom.Boost? =
        message.reactions?.let { nnReactions ->
            if (nnReactions.isEmpty()) {
                null
            } else {
                val set: MutableSet<BoostReactionImageHolder> = LinkedHashSet(1)
                var total: Long = 0
                for (reaction in nnReactions) {
//                    if (chatType?.isConversation() != true) {
//                        reaction.senderPic?.value?.let { url ->
//                            set.add(SenderPhotoUrl(url))
//                        } ?: reaction.senderAlias?.value?.let { alias ->
//                            set.add(SenderInitials(alias.getInitials()))
//                        }
//                    }
                    total += reaction.amount.value
                }

//                if (chatType?.isConversation() == true) {
//
//                    // TODO: Use Account Owner Initial Holder depending on sent/received
//                    @Exhaustive
//                    when (initialHolder) {
//                        is InitialHolderViewState.Initials -> {
//                            set.add(SenderInitials(initialHolder.initials))
//                        }
//                        is InitialHolderViewState.None -> {}
//                        is InitialHolderViewState.Url -> {
//                            set.add(SenderPhotoUrl(initialHolder.photoUrl.value))
//                        }
//                    }
//                }

                LayoutState.Bubble.ContainerBottom.Boost(
                    totalAmount = Sat(total),
                    senderPics = set,
                )
            }
        }

    val bubbleReplyMessage: LayoutState.Bubble.ReplyMessage? by lazy {
        message.replyMessage?.let { nnMessage ->
            LayoutState.Bubble.ReplyMessage(
                messageSenderName(nnMessage),

                nnMessage.retrieveTextToShow() ?: "",
            )
        }
    }

    class Sent(
        message: Message,
        chat: Chat,
        background: BubbleBackground,
        replyMessageSenderName: (Message) -> String,
    ): MessageHolderViewState(
        message,
        chat,
        background,
        InitialHolderViewState.None,
        replyMessageSenderName,
    )

    class Received(
        message: Message,
        chat: Chat,
        background: BubbleBackground,
        initialHolder: InitialHolderViewState,
        replyMessageSenderName: (Message) -> String,
    ): MessageHolderViewState(
        message,
        chat,
        background,
        initialHolder,
        replyMessageSenderName,
    )
}
