package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.model.MessageLinkPreview
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.selected.MenuItemState
import chat.sphinx.chat_common.util.SphinxLinkify
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.invoiceExpirationTimeFormat
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.isProvisionalMessage
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.MessageMedia
import chat.sphinx.wrapper_message_media.isAudio
import chat.sphinx.wrapper_message_media.isImage
import chat.sphinx.wrapper_message_media.isSphinxText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO: Remove
inline val Message.isCopyLinkAllowed: Boolean
    get() = retrieveTextToShow()?.let {
        SphinxLinkify.SphinxPatterns.COPYABLE_LINKS.matcher(it).find()
    } ?: false

inline val Message.shouldAdaptBubbleWidth: Boolean
    get() = type.isMessage() &&
            replyUUID == null &&
            !isCopyLinkAllowed &&
            !status.isDeleted()

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
    private val messageSenderInfo: (Message) -> Triple<PhotoUrl?, ContactAlias?, String>,
    private val accountOwner: () -> Contact,
    private val previewProvider: suspend (link: MessageLinkPreview) -> LayoutState.Bubble.ContainerThird.LinkPreview?,
    private val paidTextAttachmentContentProvider: suspend (message: Message) -> LayoutState.Bubble.ContainerThird.Message?,
    private val onBindDownloadMedia: () -> Unit,
) {

    companion object {
        val unsupportedMessageTypes: List<MessageType> by lazy {
            listOf(
                MessageType.Attachment,
                MessageType.Payment,
                MessageType.GroupAction.TribeDelete,
            )
        }
    }

    val unsupportedMessageType: LayoutState.Bubble.ContainerThird.UnsupportedMessageType? by lazy(LazyThreadSafetyMode.NONE) {
        if (
            unsupportedMessageTypes.contains(message.type) && message.messageMedia?.mediaType?.isSphinxText != true &&
            message.messageMedia?.mediaType?.isImage != true && message.messageMedia?.mediaType?.isAudio != true
        ) {
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
                if (initialHolder is InitialHolderViewState.Initials) initialHolder.colorKey else message.getColorKey(),
                this is Sent,
                this is Sent && message.id.isProvisionalMessage && message.status.isPending(),
                this is Sent && (message.status.isReceived() || message.status.isConfirmed()),
                this is Sent && message.status.isFailed(),
                message.messageContentDecrypted != null || message.messageMedia?.mediaKeyDecrypted != null,
                message.date.chatTimeFormat(),
            )
        } else {
            null
        }
    }

    val invoiceExpirationHeader: LayoutState.InvoiceExpirationHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.type.isInvoice()) {
            LayoutState.InvoiceExpirationHeader(
                message.isExpiredInvoice,
                message.status.isConfirmed(),
                message.expirationDate?.invoiceExpirationTimeFormat(),
                this is Sent,
            )
        } else {
            null
        }
    }

    val deletedMessage: LayoutState.DeletedMessage? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.status.isDeleted()) {
            LayoutState.DeletedMessage(
                gravityStart = this is Received,
                timestamp = message.date.chatTimeFormat()
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

    val bubbleInvoice: LayoutState.Bubble.ContainerSecond.Invoice? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.type.isInvoice()) {
            LayoutState.Bubble.ContainerSecond.Invoice(
                showSent = this is Sent,
                amount = message.amount,
                text = message.retrieveInvoiceTextToShow() ?: "",
                isExpired = message.isExpiredInvoice,
                isPaid = message.status.isConfirmed(),
            )
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

    val bubblePaidMessage: LayoutState.Bubble.ContainerThird.PaidMessage? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.retrieveTextToShow() != null || !message.isPaidTextMessage) {
            null
        } else {
            val purchaseStatus = message.retrievePurchaseStatus()

            if (this is Sent) {
                LayoutState.Bubble.ContainerThird.PaidMessage(
                    true,
                    purchaseStatus
                )
            } else {
                LayoutState.Bubble.ContainerThird.PaidMessage(
                    false,
                    purchaseStatus
                )
            }
        }
    }

    val bubbleCallInvite: LayoutState.Bubble.ContainerSecond.CallInvite? by lazy(LazyThreadSafetyMode.NONE) {
        message.retrieveSphinxCallLink()?.let { callLink ->
            LayoutState.Bubble.ContainerSecond.CallInvite(!callLink.startAudioOnly)
        }
    }

    val bubbleBotResponse: LayoutState.Bubble.ContainerSecond.BotResponse? by lazy(LazyThreadSafetyMode.NONE) {
        if (message.type.isBotRes()) {
            message.retrieveBotResponseHtmlString()?.let { html ->
                LayoutState.Bubble.ContainerSecond.BotResponse(
                    html
                )
            }
        } else {
            null
        }
    }

    val bubblePaidMessageReceivedDetails: LayoutState.Bubble.ContainerFourth.PaidMessageReceivedDetails? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage || this is Sent) {
            null
        } else {
            message.retrievePurchaseStatus()?.let { purchaseStatus ->
                LayoutState.Bubble.ContainerFourth.PaidMessageReceivedDetails(
                    amount = message.messageMedia?.price ?: Sat(0),
                    purchaseStatus = purchaseStatus,
                    showStatusIcon = purchaseStatus.isPurchaseAccepted() ||
                            purchaseStatus.isPurchaseDenied(),
                    showProcessingProgressBar = purchaseStatus.isPurchaseProcessing(),
                    showStatusLabel = purchaseStatus.isPurchaseProcessing() ||
                            purchaseStatus.isPurchaseAccepted() ||
                            purchaseStatus.isPurchaseDenied(),
                    showPayElements = purchaseStatus.isPurchasePending()
                )
            }
        }
    }

    val bubblePaidMessageSentStatus: LayoutState.Bubble.ContainerSecond.PaidMessageSentStatus? by lazy(LazyThreadSafetyMode.NONE) {
        if (!message.isPaidMessage || this !is Sent) {
            null
        } else {
            message.retrievePurchaseStatus()?.let { purchaseStatus ->
                LayoutState.Bubble.ContainerSecond.PaidMessageSentStatus(
                    amount = message.messageMedia?.price ?: Sat(0),
                    purchaseStatus = purchaseStatus
                )
            }
        }
    }

    val bubbleAudioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment? by lazy(LazyThreadSafetyMode.NONE) {
        message.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isAudio) {

                nnMessageMedia.localFile?.let { nnFile ->

                    LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable(nnFile)

                } ?: run {
                    val pendingPayment = this is Received && message.isPaidPendingMessage

                    // will only be called once when value is lazily initialized upon binding
                    // data to view.
                    if (!pendingPayment) {
                        onBindDownloadMedia.invoke()
                    }

                    LayoutState.Bubble.ContainerSecond.AudioAttachment.FileUnavailable(pendingPayment)
                }
            } else {
                null
            }
        }
//        message.retrieveAudioUrlAndMessageMedia()?.let { mediaData ->
//            LayoutState.Bubble.ContainerSecond.AudioAttachment(
//                mediaData.first,
//                mediaData.second,
//                (this is Received && message.isPaidPendingMessage)
//            )
//        }
    }

    val bubbleImageAttachment: LayoutState.Bubble.ContainerSecond.ImageAttachment? by lazy(LazyThreadSafetyMode.NONE) {
        message.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
            LayoutState.Bubble.ContainerSecond.ImageAttachment(
                mediaData.first,
                mediaData.second,
                (this is Received && message.isPaidPendingMessage)
            )
        }
    }

    val bubblePodcastBoost: LayoutState.Bubble.ContainerSecond.PodcastBoost? by lazy(LazyThreadSafetyMode.NONE) {
        message.podBoost?.let { podBoost ->
            LayoutState.Bubble.ContainerSecond.PodcastBoost(
                podBoost.amount,
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
                val set: MutableSet<BoostSenderHolder> = LinkedHashSet(0)
                var total: Long = 0
                var boostedByOwner = false
                val owner = accountOwner()

                for (reaction in nnReactions) {
                    if (reaction.sender == owner.id) {
                        boostedByOwner = true

                        set.add(BoostSenderHolder(
                            owner.photoUrl,
                            owner.alias,
                            owner.getColorKey()
                        ))
                    } else {
                        if (chat.type.isConversation()) {
                            val senderInfo = messageSenderInfo(reaction)

                            set.add(BoostSenderHolder(
                                senderInfo.first,
                                senderInfo.second,
                                senderInfo.third
                            ))
                        } else {
                            set.add(BoostSenderHolder(
                                reaction.senderPic,
                                reaction.senderAlias?.value?.toContactAlias(),
                                reaction.getColorKey()
                            ))
                        }
                    }
                    total += reaction.amount.value
                }

                LayoutState.Bubble.ContainerFourth.Boost(
                    showSent = (this is Sent),
                    boostedByOwner = boostedByOwner,
                    senders = set,
                    totalAmount = Sat(total),
                )
            }
        }

    val bubbleReplyMessage: LayoutState.Bubble.ContainerFirst.ReplyMessage? by lazy {
        message.replyMessage?.let { nnReplyMessage ->

            var mediaUrl: String? = null
            var messageMedia: MessageMedia? = null

            nnReplyMessage.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
                mediaUrl = mediaData.first
                messageMedia = mediaData.second
            }

            LayoutState.Bubble.ContainerFirst.ReplyMessage(
                showSent = this is Sent,
                messageSenderInfo(nnReplyMessage).second?.value ?: "",
                nnReplyMessage.getColorKey(),
                nnReplyMessage.retrieveTextToShow() ?: "",
                nnReplyMessage.isAudioMessage,
                mediaUrl,
                messageMedia
            )
        }
    }

    val groupActionIndicator: LayoutState.GroupActionIndicator? by lazy(LazyThreadSafetyMode.NONE) {
        val type = message.type
        if (!type.isGroupAction()) {
            null
        } else {
            LayoutState.GroupActionIndicator(
                actionType = type,
                isAdminView = if (chat.ownerPubKey == null || accountOwner().nodePubKey == null) {
                    false
                } else {
                    chat.ownerPubKey == accountOwner().nodePubKey
                },
                chatType = chat.type,
                subjectName = message.senderAlias?.value ?: ""
            )
        }
    }

    val messageLinkPreview: MessageLinkPreview? by lazy {
        MessageLinkPreview.parse(bubbleMessage)
    }

    @Volatile
    private var linkPreviewLayoutState: LayoutState.Bubble.ContainerThird.LinkPreview? = null
    private val previewLock = Mutex()
    suspend fun retrieveLinkPreview(): LayoutState.Bubble.ContainerThird.LinkPreview? {
        return messageLinkPreview?.let { nnPreview ->
            linkPreviewLayoutState ?: previewLock.withLock {
                linkPreviewLayoutState ?: previewProvider.invoke(nnPreview)
                    ?.also { linkPreviewLayoutState = it }
            }
        }
    }

    private val paidTextMessageContentLock = Mutex()
    suspend fun retrievePaidTextMessageContent(): LayoutState.Bubble.ContainerThird.Message? {
        return bubbleMessage ?: paidTextMessageContentLock.withLock {
            bubbleMessage ?: paidTextAttachmentContentProvider.invoke(message)
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

            if (message.isMediaAttachmentAvailable) {
                list.add(MenuItemState.SaveFile)
            }

            if (message.isCopyLinkAllowed) {
                list.add(MenuItemState.CopyLink)
            }

            if (message.isCopyAllowed) {
                list.add(MenuItemState.CopyText)
            }

            if (message.isReplyAllowed) {
                list.add(MenuItemState.Reply)
            }

            if (message.isResendAllowed) {
                list.add(MenuItemState.Resend)
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
        messageSenderInfo: (Message) -> Triple<PhotoUrl?, ContactAlias?, String>,
        accountOwner: () -> Contact,
        previewProvider: suspend (link: MessageLinkPreview) -> LayoutState.Bubble.ContainerThird.LinkPreview?,
        paidTextMessageContentProvider: suspend (message: Message) -> LayoutState.Bubble.ContainerThird.Message?,
        onBindDownloadMedia: () -> Unit,
    ) : MessageHolderViewState(
        message,
        chat,
        background,
        InitialHolderViewState.None,
        messageSenderInfo,
        accountOwner,
        previewProvider,
        paidTextMessageContentProvider,
        onBindDownloadMedia,
    )

    class Received(
        message: Message,
        chat: Chat,
        background: BubbleBackground,
        initialHolder: InitialHolderViewState,
        messageSenderInfo: (Message) -> Triple<PhotoUrl?, ContactAlias?, String>,
        accountOwner: () -> Contact,
        previewProvider: suspend (link: MessageLinkPreview) -> LayoutState.Bubble.ContainerThird.LinkPreview?,
        paidTextMessageContentProvider: suspend (message: Message) -> LayoutState.Bubble.ContainerThird.Message?,
        onBindDownloadMedia: () -> Unit,
    ) : MessageHolderViewState(
        message,
        chat,
        background,
        initialHolder,
        messageSenderInfo,
        accountOwner,
        previewProvider,
        paidTextMessageContentProvider,
        onBindDownloadMedia,
    )
}
