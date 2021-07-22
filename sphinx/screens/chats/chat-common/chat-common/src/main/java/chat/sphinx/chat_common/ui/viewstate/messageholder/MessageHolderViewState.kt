package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.selected.MenuItemState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.MessageMedia
import chat.sphinx.wrapper_message_media.isImage

internal inline val MessageHolderViewState.isReceived: Boolean
    get() = this is MessageHolderViewState.Received

internal inline val MessageHolderViewState.showReceivedBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Received

internal val MessageHolderViewState.showSentBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Sent

internal sealed class MessageHolderViewState(
    val message: Message,
    chat: Chat,
    val background: BubbleBackground,
    val initialHolder: InitialHolderViewState,
    val messageSenderName: (Message) -> String,
    val accountOwner: () -> Contact,
) {

    companion object {
        val unsupportedMessageTypes: List<MessageType> by lazy {
            listOf(
                MessageType.Attachment,
                MessageType.BotRes,
                MessageType.Invoice,
                MessageType.Payment,
                MessageType.GroupAction.TribeDelete,
            )
        }
    }

    val unsupportedMessageType: LayoutState.Bubble.ContainerThird.UnsupportedMessageType? by lazy(LazyThreadSafetyMode.NONE) {
        if (unsupportedMessageTypes.contains(message.type) && message.messageMedia?.mediaType?.isImage != true) {
            LayoutState.Bubble.ContainerThird.UnsupportedMessageType(
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
                message.messageContentDecrypted != null || message.messageMedia?.mediaKeyDecrypted != null,
                message.date.chatTimeFormat(),
            )
        } else {
            null
        }
    }

    val deletedMessage: LayoutState.DeletedMessage? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.status.isDeleted()) {
            LayoutState.DeletedMessage(
                gravityStart = this is Received,
                timestamp = DateTime.getFormathmma().format(message.date.chatTimeFormat())
            )
        } else {
            null
        }
    }

    val bubbleDirectPayment: LayoutState.Bubble.ContainerSecond.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.type.isDirectPayment()) {
            LayoutState.Bubble.ContainerSecond.DirectPayment(showSent = this is Sent, amount = message.amount)
        } else {
            null
        }
    }

    val bubbleMessage: LayoutState.Bubble.ContainerThird.Message? by lazy(LazyThreadSafetyMode.NONE) {
        message.retrieveTextToShow()?.let { text ->
            if (text.isNotEmpty()) {
                LayoutState.Bubble.ContainerThird.Message(text = text)
            } else {
                null
            }
        }
    }

    val bubblePaidMessageDetails: LayoutState.Bubble.ContainerFourth.PaidMessageDetails? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage) {
            null
        } else {
            val isPaymentPending = message.status.isPending()

            message.type.let { type ->
                LayoutState.Bubble.ContainerFourth.PaidMessageDetails(
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

    val bubblePaidMessageSentStatus: LayoutState.Bubble.ContainerSecond.PaidMessageSentStatus? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage || this !is Sent) {
            null
        } else {
            message.type.let { type ->
                LayoutState.Bubble.ContainerSecond.PaidMessageSentStatus(
                    amount = message.amount,
                    purchaseType = if (type.isPurchase()) type else null,
                )
            }
        }
    }

    val bubbleImageAttachment: LayoutState.Bubble.ContainerSecond.ImageAttachment? by lazy(LazyThreadSafetyMode.NONE) {
        message.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
            LayoutState.Bubble.ContainerSecond.ImageAttachment(
                mediaData.first,
                mediaData.second
            )
        }
    }

    // don't use by lazy as this uses a for loop and needs to be initialized on a background
    // thread (so, while the MHVS is being created)
    val bubbleReactionBoosts: LayoutState.Bubble.ContainerFourth.Boost? =
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

                LayoutState.Bubble.ContainerFourth.Boost(
                    totalAmount = Sat(total),
                    senderPics = set,
                )
            }
        }

    val bubbleReplyMessage: LayoutState.Bubble.ContainerFirst.ReplyMessage? by lazy {
        message.replyMessage?.let { nnMessage ->
            var mediaUrl: String? = null
            var messageMedia: MessageMedia? = null

            nnMessage.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
                mediaUrl = mediaData.first
                messageMedia = mediaData.second
            }

            LayoutState.Bubble.ContainerFirst.ReplyMessage(
                showSent = this is Sent,
                messageSenderName(nnMessage),
                nnMessage.retrieveTextToShow() ?: "",
                mediaUrl,
                messageMedia
            )
        }
    }

    val groupActionIndicator: LayoutState.GroupActionIndicator? by lazy(LazyThreadSafetyMode.NONE) {
        if (
            !message.type.isGroupAction() ||
            message.senderAlias == null
        ) {
            null
        } else {
            LayoutState.GroupActionIndicator(
                actionType = message.type as MessageType.GroupAction,
                isAdminView = if (chat.ownerPubKey == null || accountOwner().nodePubKey == null) {
                    false
                } else {
                    chat.ownerPubKey == accountOwner().nodePubKey
                },
                chatType = chat.type,
                subjectName = message.senderAlias!!.value
            )
        }
    }

    val selectionMenuItems: List<MenuItemState>? by lazy(LazyThreadSafetyMode.NONE) {
        if (
            background is BubbleBackground.Gone         ||
            message.podBoost != null
        ) {
            null
        } else {
            // TODO: check message status

            val list = ArrayList<MenuItemState>(4)

            if (this is Received && message.isBoostAllowed) {
                list.add(MenuItemState.Boost)
            }

            if (message.isCopyAllowed) {
                list.add(MenuItemState.CopyText)
            }

            if (message.isReplyAllowed) {
                list.add(MenuItemState.Reply)
            }

            if (this is Sent || chat.isTribeOwnedByAccount(accountOwner().nodePubKey)) {
                list.add(MenuItemState.Delete)
            }

            if (list.isEmpty()) {
                null
            } else {
                list.sortBy { it.sortPriority }
                list
            }
        }
    }

    class Sent(
        message: Message,
        chat: Chat,
        background: BubbleBackground,
        replyMessageSenderName: (Message) -> String,
        accountOwner: () -> Contact,
    ) : MessageHolderViewState(
        message,
        chat,
        background,
        InitialHolderViewState.None,
        replyMessageSenderName,
        accountOwner,
    )

    class Received(
        message: Message,
        chat: Chat,
        background: BubbleBackground,
        initialHolder: InitialHolderViewState,
        replyMessageSenderName: (Message) -> String,
        accountOwner: () -> Contact,
    ) : MessageHolderViewState(
        message,
        chat,
        background,
        initialHolder,
        replyMessageSenderName,
        accountOwner,
    )
}
