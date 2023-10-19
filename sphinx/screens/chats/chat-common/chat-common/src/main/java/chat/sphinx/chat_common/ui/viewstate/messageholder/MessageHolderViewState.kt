package chat.sphinx.chat_common.ui.viewstate.messageholder

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.util.Log
import chat.sphinx.chat_common.model.MessageLinkPreview
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.selected.MenuItemState
import chat.sphinx.chat_common.util.SphinxLinkify
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.isConversation
import chat.sphinx.wrapper_chat.isTribe
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.isProvisionalMessage
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


// TODO: Remove
inline val Message.isCopyLinkAllowed: Boolean
    get() = retrieveTextToShow()?.let {
        SphinxLinkify.SphinxPatterns.COPYABLE_LINKS.matcher(it).find()
    } ?: false

inline val Message.shouldAdaptBubbleWidth: Boolean
    get() = (type.isMessage() &&
            !isSphinxCallLink &&
            podcastClip == null &&
            replyUUID == null &&
            !isCopyLinkAllowed &&
            (thread == null || thread!!.isEmpty()) &&
            !status.isDeleted() &&
            !flagged.isTrue()) ||
            type.isDirectPayment()

internal inline val MessageHolderViewState.isReceived: Boolean
    get() = this is MessageHolderViewState.Received

internal inline val MessageHolderViewState.showReceivedBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Received

internal val MessageHolderViewState.showSentBubbleArrow: Boolean
    get() = background is BubbleBackground.First && this is MessageHolderViewState.Sent

internal sealed class MessageHolderViewState(
    val message: Message?,
    chat: Chat,
    private val tribeAdmin: Contact?,
    val messageHolderType: MessageHolderType,
    private val separatorDate: DateTime?,
    val background: BubbleBackground,
    val invoiceLinesHolderViewState: InvoiceLinesHolderViewState,
    val initialHolder: InitialHolderViewState,
    var highlightedText: String?,
    private val messageSenderInfo: (Message) -> Triple<PhotoUrl?, ContactAlias?, String>?,
    private val accountOwner: () -> Contact,
    private val urlLinkPreviewsEnabled: Boolean,
    private val previewProvider: suspend (link: MessageLinkPreview) -> LayoutState.Bubble.ContainerThird.LinkPreview?,
    private val paidTextAttachmentContentProvider: suspend (message: Message) -> LayoutState.Bubble.ContainerThird.Message?,
    private val onBindDownloadMedia: () -> Unit,
    private val onBindThreadDownloadMedia: () -> Unit,
) {

    val isPinned: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        (message?.uuid?.value == chat.pinedMessage?.value)
    }

    val searchHighlightedStatus: LayoutState.SearchHighlightedStatus?
    get() = if (highlightedText != null && highlightedText?.isEmpty() == false) {
                LayoutState.SearchHighlightedStatus(
                    highlightedText!!
                )
            } else {
                null
            }


    val unsupportedMessageType: LayoutState.Bubble.ContainerThird.UnsupportedMessageType? by lazy(LazyThreadSafetyMode.NONE) {
        null
    }

    val messagesSeparator: LayoutState.Separator? by lazy(LazyThreadSafetyMode.NONE) {
        if (message != null) {
            null
        } else {
            LayoutState.Separator(
                messageHolderType,
                separatorDate
            )
        }
    }

    val statusHeader: LayoutState.MessageStatusHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            val isFirstBubble = (background is BubbleBackground.First)
            val isInvoicePayment = (message.type.isInvoicePayment() && message.status.isConfirmed())

            if (isFirstBubble || isInvoicePayment) {
                LayoutState.MessageStatusHeader(
                    if (chat.type.isConversation()) null else message.senderAlias?.value,
                    if (initialHolder is InitialHolderViewState.Initials) initialHolder.colorKey else message.getColorKey(),
                    this is Sent,
                    this is Sent && message.id.isProvisionalMessage && message.status.isPending(),
                    this is Sent && (message.status.isReceived() || message.status.isConfirmed()),
                    this is Sent && message.status.isFailed(),
                    message.messageContentDecrypted != null || message.messageMedia?.mediaKeyDecrypted != null,
                    message.date.messageTimeFormat(),
                    message.errorMessage?.value?.trim()
                )
            } else {
                null
            }
        }
    }

    val invoiceExpirationHeader: LayoutState.InvoiceExpirationHeader? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            if (message.type.isInvoice() && !message.status.isDeleted()) {
                LayoutState.InvoiceExpirationHeader(
                    showExpirationReceivedHeader = !message.isPaidInvoice && this is Received,
                    showExpirationSentHeader = !message.isPaidInvoice && this is Sent,
                    showExpiredLabel = message.isExpiredInvoice,
                    showExpiresAtLabel = !message.isExpiredInvoice && !message.isPaidInvoice,
                    expirationTimestamp = message.expirationDate?.invoiceExpirationTimeFormat(),
                )
            } else {
                null
            }
        }
    }

    val deletedOrFlaggedMessage: LayoutState.DeletedOrFlaggedMessage? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            if (message.status.isDeleted() || message.isFlagged) {
                LayoutState.DeletedOrFlaggedMessage(
                    gravityStart = this is Received,
                    deleted = message.status.isDeleted(),
                    flagged = message.isFlagged,
                    timestamp = message.date.chatTimeFormat()
                )
            } else {
                null
            }
        }
    }

    val invoicePayment: LayoutState.InvoicePayment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            if (message.type.isInvoicePayment()) {
                LayoutState.InvoicePayment(
                    showSent = this is Sent,
                    paymentDateString = message.date.invoicePaymentDateFormat()
                )
            } else {
                null
            }
        }
    }

    val bubbleDirectPayment: LayoutState.Bubble.ContainerSecond.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            if (message.type.isDirectPayment()) {
                LayoutState.Bubble.ContainerSecond.DirectPayment(
                    showSent = this is Sent,
                    amount = message.amount,
                    isTribe = chat.isTribe(),
                    recipientAlias = message.recipientAlias,
                    recipientPic = message.recipientPic,
                    recipientColorKey = message.getRecipientColorKey(
                        tribeAdminId = tribeAdmin?.id ?: ContactId(-1),
                        recipientAlias = message.recipientAlias
                    )
                )
            } else {
                null
            }
        }
    }

    val bubbleInvoice: LayoutState.Bubble.ContainerSecond.Invoice? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            if (message.type.isInvoice()) {
                LayoutState.Bubble.ContainerSecond.Invoice(
                    showSent = this is Sent,
                    amount = message.amount,
                    text = message.retrieveInvoiceTextToShow() ?: "",
                    showPaidInvoiceBottomLine = message.isPaidInvoice,
                    hideBubbleArrows = !message.isExpiredInvoice && !message.isPaidInvoice,
                    showPayButton = !message.isExpiredInvoice && !message.isPaidInvoice && this is Received,
                    showDashedBorder = !message.isExpiredInvoice && !message.isPaidInvoice,
                    showExpiredLayout = message.isExpiredInvoice
                )
            } else {
                null
            }
        }
    }

    val bubbleMessage: LayoutState.Bubble.ContainerThird.Message? by  lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            val isThread = message.thread?.isNotEmpty() == true

            message.retrieveTextToShow()?.let { text ->
                if (text.isNotEmpty()) {
                    LayoutState.Bubble.ContainerThird.Message(
                        text = text,
                        decryptionError = false,
                        isThread = isThread
                    )
                } else {
                    null
                }
            } ?: message.messageDecryptionError?.let { decryptionError ->
                if (decryptionError) {
                    LayoutState.Bubble.ContainerThird.Message(
                        text = null,
                        decryptionError = true,
                        isThread = isThread
                    )
                } else {
                    null
                }
            }
        }
    }

    val bubbleThread: LayoutState.Bubble.ContainerThird.Thread? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            message.thread?.let { replies ->
                if (replies.isEmpty() || chat.isConversation()){
                    null
                } else {
                    val users: MutableList<ReplyUserHolder> = mutableListOf()

                    val owner = accountOwner()
                    val ownerUserHolder = ReplyUserHolder(
                        owner.photoUrl,
                        owner.alias,
                        owner.getColorKey()
                    )

                    replies.forEach { replyMessage ->
                        users.add(
                            if (replyMessage.sender == owner.id) {
                                ownerUserHolder
                            } else {
                                ReplyUserHolder(
                                    replyMessage.senderPic,
                                    replyMessage.senderAlias?.value?.toContactAlias(),
                                    replyMessage.getColorKey()
                                )
                            }
                        )
                    }

                    val lastReplyUser = replies.last().let {
                        if (it.sender == owner.id) {
                            ownerUserHolder
                        } else {
                            ReplyUserHolder(
                                it.senderPic,
                                it.senderAlias?.value?.toContactAlias(),
                                it.getColorKey()
                            )
                        }
                    }

                    val sent = message.sender == chat.contactIds.firstOrNull()

                    val lastReplyAttachment: LayoutState.Bubble.ContainerSecond? =
                        when(replies.last().messageMedia?.mediaType) {
                            is MediaType.Image -> {
                                getImageAttachment(
                                    replies.last(),
                                    true
                                )
                            }
                            is MediaType.Video -> {
                                getVideoAttachment(
                                    replies.last(),
                                    true
                                )
                            }
                            is MediaType.Audio -> {
                                getAudioAttachment(
                                    replies.last(),
                                    true
                                )
                            }
                            is MediaType.Pdf -> {
                                getFileAttachment(
                                    replies.last(),
                                    true
                                )
                            }
                            else -> { null }
                        }

                    LayoutState.Bubble.ContainerThird.Thread(
                        replyCount = replies.size,
                        users = users,
                        lastReplyMessage = replies.last().retrieveTextToShow(),
                        lastReplyDate = replies.last().date.chatTimeFormat(),
                        lastReplyUser = lastReplyUser,
                        isSentMessage = sent,
                        mediaAttachment = lastReplyAttachment
                    )
                }
            }
        }
    }


    val bubblePaidMessage: LayoutState.Bubble.ContainerThird.PaidMessage? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null || message.retrieveTextToShow() != null || !message.isPaidTextMessage) {
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
        if (message == null) {
            null
        } else {
            message.retrieveSphinxCallLink()?.let { callLink ->
                LayoutState.Bubble.ContainerSecond.CallInvite(!callLink.startAudioOnly)
            }
        }
    }

    val bubbleBotResponse: LayoutState.Bubble.ContainerSecond.BotResponse? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
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
    }

    val bubblePaidMessageReceivedDetails: LayoutState.Bubble.ContainerFourth.PaidMessageReceivedDetails? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null || !message.isPaidMessage || this is Sent) {
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
        if (message == null || !message.isPaidMessage || this !is Sent) {
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
        if (message == null) {
            null
        } else {
            getAudioAttachment(message)
        }
    }

    val bubblePodcastClip: LayoutState.Bubble.ContainerSecond.PodcastClip? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            message.podcastClip?.let { nnPodcastClip ->
                LayoutState.Bubble.ContainerSecond.PodcastClip(
                    message.id,
                    message.uuid,
                    nnPodcastClip
                )
            }
        }
    }

    val bubbleImageAttachment: LayoutState.Bubble.ContainerSecond.ImageAttachment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            getImageAttachment(message)
        }
    }

    val bubbleVideoAttachment: LayoutState.Bubble.ContainerSecond.VideoAttachment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            getVideoAttachment(message)
        }
    }


    val bubbleFileAttachment: LayoutState.Bubble.ContainerSecond.FileAttachment? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null){
            null
        } else {
            getFileAttachment(message)
        }
    }

    val bubblePodcastBoost: LayoutState.Bubble.ContainerSecond.PodcastBoost? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
            message.feedBoost?.let { podBoost ->
                LayoutState.Bubble.ContainerSecond.PodcastBoost(
                    podBoost.amount,
                )
            }
        }
    }

    // don't use by lazy as this uses a for loop and needs to be initialized on a background
    // thread (so, while the MHVS is being created)
    val bubbleReactionBoosts: LayoutState.Bubble.ContainerFourth.Boost? by lazy {
        if (message == null) {
            null
        } else {
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

                            set.add(
                                BoostSenderHolder(
                                    photoUrl = chat.myPhotoUrl ?: owner.photoUrl,
                                    alias = chat.myAlias?.value?.toContactAlias() ?: owner.alias,
                                    colorKey = owner.getColorKey()
                                )
                            )
                        } else {
                            if (chat.type.isConversation()) {
                                messageSenderInfo(reaction)?.let { senderInfo ->
                                    set.add(
                                        BoostSenderHolder(
                                            senderInfo.first,
                                            senderInfo.second,
                                            senderInfo.third
                                        )
                                    )
                                }
                            } else {
                                set.add(
                                    BoostSenderHolder(
                                        reaction.senderPic,
                                        reaction.senderAlias?.value?.toContactAlias(),
                                        reaction.getColorKey()
                                    )
                                )
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
        }
    }

    val bubbleReplyMessage: LayoutState.Bubble.ContainerFirst.ReplyMessage? by lazy {
        if (message == null) {
            null
        } else {
            message.replyMessage?.let { nnReplyMessage ->

                var mediaUrl: String? = null
                var messageMedia: MessageMedia? = null

                nnReplyMessage.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
                    mediaUrl = mediaData.first
                    messageMedia = mediaData.second
                }

                LayoutState.Bubble.ContainerFirst.ReplyMessage(
                    showSent = this is Sent,
                    messageSenderInfo(nnReplyMessage)?.second?.value ?: "",
                    nnReplyMessage.getColorKey(),
                    nnReplyMessage.retrieveTextToShow() ?: "",
                    nnReplyMessage.isAudioMessage,
                    mediaUrl,
                    messageMedia
                )
            }
        }
    }

    val groupActionIndicator: LayoutState.GroupActionIndicator? by lazy(LazyThreadSafetyMode.NONE) {
        if (message == null) {
            null
        } else {
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
    }

    val messageLinkPreview: MessageLinkPreview? by lazy {
        MessageLinkPreview.parse(
            bubbleMessage,
            urlLinkPreviewsEnabled
        )
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
        return if (message == null) {
            null
        } else {
            bubbleMessage ?: paidTextMessageContentLock.withLock {
                bubbleMessage ?: paidTextAttachmentContentProvider.invoke(message)
            }
        }
    }

    val selectionMenuItems: List<MenuItemState>? by lazy(LazyThreadSafetyMode.NONE) {
        if (
            message != null &&
            (background is BubbleBackground.Gone         ||
            message.feedBoost != null)
        ) {
            null
        } else {
            // TODO: check message status

            val list = ArrayList<MenuItemState>(4)
            val nnMessage = message!!

            if (this is Received && nnMessage.isBoostAllowed) {
                list.add(MenuItemState.Boost)
            }

            if (nnMessage.isMediaAttachmentAvailable) {
                list.add(MenuItemState.SaveFile)
            }

            if (nnMessage.isCopyLinkAllowed) {
                list.add(MenuItemState.CopyLink)
            }

            if (nnMessage.isCopyAllowed) {
                list.add(MenuItemState.CopyText)
            }

            if (nnMessage.isReplyAllowed) {
                list.add(MenuItemState.Reply)
            }

            if (nnMessage.isResendAllowed) {
                list.add(MenuItemState.Resend)
            }

            if (this is Sent || chat.isTribeOwnedByAccount(accountOwner().nodePubKey)) {
                list.add(MenuItemState.Delete)
            }

            if (this is Received) {
                list.add(MenuItemState.Flag)
            }

            if(chat.isTribeOwnedByAccount(accountOwner().nodePubKey)) {
                if (nnMessage.isPinAllowed(chat.pinedMessage)) {
                    list.add(MenuItemState.PinMessage)
                }

                if (nnMessage.isUnPinAllowed(chat.pinedMessage)) {
                    list.add(MenuItemState.UnpinMessage)
                }
            }


            if (list.isEmpty()) {
                null
            } else {
                list.sortBy { it.sortPriority }
                list
            }
        }
    }

    private fun getImageAttachment(
        message: Message,
        isThread: Boolean = false
    ): LayoutState.Bubble.ContainerSecond.ImageAttachment? {
        val pendingPayment = this is Received && message.isPaidPendingMessage

        if (!pendingPayment) {
            if (isThread) {
                onBindThreadDownloadMedia.invoke()
            } else {
                onBindDownloadMedia.invoke()
            }
        }
        val isThread = message.thread?.isNotEmpty() ?: false

        return message.retrieveImageUrlAndMessageMedia()?.let { mediaData ->
            LayoutState.Bubble.ContainerSecond.ImageAttachment(
                mediaData.first,
                mediaData.second,
                pendingPayment,
                isThread
            )
        }
    }

    private fun getVideoAttachment(
        message: Message,
        isThread: Boolean = false
    ): LayoutState.Bubble.ContainerSecond.VideoAttachment? {
        return message.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isVideo) {
                nnMessageMedia.localFile?.let { nnFile ->
                    LayoutState.Bubble.ContainerSecond.VideoAttachment.FileAvailable(nnFile)
                } ?: run {
                    val pendingPayment = this is Received && message.isPaidPendingMessage

                    // will only be called once when value is lazily initialized upon binding
                    // data to view.
                    if (!pendingPayment) {
                        if (isThread) {
                            onBindThreadDownloadMedia.invoke()
                        } else {
                            onBindDownloadMedia.invoke()
                        }
                    }

                    LayoutState.Bubble.ContainerSecond.VideoAttachment.FileUnavailable(
                        pendingPayment
                    )
                }
            } else {
                null
            }
        }
    }

    private fun getFileAttachment(
        message: Message,
        isThread: Boolean = false
    ): LayoutState.Bubble.ContainerSecond.FileAttachment? {
        return message.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isPdf || nnMessageMedia.mediaType.isUnknown) {

                nnMessageMedia.localFile?.let { nnFile ->

                    val pageCount = if (nnMessageMedia.mediaType.isPdf) {
                        val fileDescriptor = ParcelFileDescriptor.open(nnFile, MODE_READ_ONLY)
                        val renderer = PdfRenderer(fileDescriptor)
                        renderer.pageCount
                    } else {
                        0
                    }

                    LayoutState.Bubble.ContainerSecond.FileAttachment.FileAvailable(
                        nnMessageMedia.fileName,
                        FileSize(nnFile.length()),
                        nnMessageMedia.mediaType.isPdf,
                        pageCount
                    )
                } ?: run {
                    val pendingPayment = this is Received && message.isPaidPendingMessage

                    if (!pendingPayment) {
                        if (isThread) {
                            onBindThreadDownloadMedia.invoke()
                        } else {
                            onBindDownloadMedia.invoke()
                        }
                    }

                    LayoutState.Bubble.ContainerSecond.FileAttachment.FileUnavailable(
                        pendingPayment
                    )

                }
            } else {
                null
            }
        }
    }

    private fun getAudioAttachment(
        message: Message,
        isThread: Boolean = false
    ): LayoutState.Bubble.ContainerSecond.AudioAttachment? {
        return message.messageMedia?.let { nnMessageMedia ->
            if (nnMessageMedia.mediaType.isAudio) {

                nnMessageMedia.localFile?.let { nnFile ->

                    LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable(
                        message.id,
                        nnFile
                    )

                } ?: run {
                    val pendingPayment = this is Received && message.isPaidPendingMessage

                    // will only be called once when value is lazily initialized upon binding
                    // data to view.
                    if (!pendingPayment) {
                        if (isThread) {
                            onBindThreadDownloadMedia.invoke()
                        } else {
                            onBindDownloadMedia.invoke()
                        }
                    }

                    LayoutState.Bubble.ContainerSecond.AudioAttachment.FileUnavailable(
                        message.id,
                        pendingPayment
                    )
                }
            } else {
                null
            }
        }

    }

    class Sent(
        message: Message,
        chat: Chat,
        tribeAdmin: Contact?,
        background: BubbleBackground,
        invoiceLinesHolderViewState: InvoiceLinesHolderViewState,
        highlightedText: String?,
        messageSenderInfo: (Message) -> Triple<PhotoUrl?, ContactAlias?, String>,
        accountOwner: () -> Contact,
        urlLinkPreviewsEnabled: Boolean,
        previewProvider: suspend (link: MessageLinkPreview) -> LayoutState.Bubble.ContainerThird.LinkPreview?,
        paidTextMessageContentProvider: suspend (message: Message) -> LayoutState.Bubble.ContainerThird.Message?,
        onBindDownloadMedia: () -> Unit,
        onBindThreadDownloadMedia: () -> Unit,
    ) : MessageHolderViewState(
        message,
        chat,
        tribeAdmin,
        MessageHolderType.Message,
        null,
        background,
        invoiceLinesHolderViewState,
        InitialHolderViewState.None,
        highlightedText,
        messageSenderInfo,
        accountOwner,
        urlLinkPreviewsEnabled,
        previewProvider,
        paidTextMessageContentProvider,
        onBindDownloadMedia,
        onBindThreadDownloadMedia
    )

    class Received(
        message: Message,
        chat: Chat,
        tribeAdmin: Contact?,
        background: BubbleBackground,
        invoiceLinesHolderViewState: InvoiceLinesHolderViewState,
        initialHolder: InitialHolderViewState,
        highlightedText: String?,
        messageSenderInfo: (Message) -> Triple<PhotoUrl?, ContactAlias?, String>,
        accountOwner: () -> Contact,
        urlLinkPreviewsEnabled: Boolean,
        previewProvider: suspend (link: MessageLinkPreview) -> LayoutState.Bubble.ContainerThird.LinkPreview?,
        paidTextMessageContentProvider: suspend (message: Message) -> LayoutState.Bubble.ContainerThird.Message?,
        onBindDownloadMedia: () -> Unit,
        onBindThreadDownloadMedia: () -> Unit,
    ) : MessageHolderViewState(
        message,
        chat,
        tribeAdmin,
        MessageHolderType.Message,
        null,
        background,
        invoiceLinesHolderViewState,
        initialHolder,
        highlightedText,
        messageSenderInfo,
        accountOwner,
        urlLinkPreviewsEnabled,
        previewProvider,
        paidTextMessageContentProvider,
        onBindDownloadMedia,
        onBindThreadDownloadMedia,
    )

    class Separator(
        messageHolderType: MessageHolderType,
        separatorDate: DateTime?,
        chat: Chat,
        tribeAdmin: Contact?,
        background: BubbleBackground,
        invoiceLinesHolderViewState: InvoiceLinesHolderViewState,
        initialHolder: InitialHolderViewState,
        accountOwner: () -> Contact,
    ) : MessageHolderViewState(
        null,
        chat,
        tribeAdmin,
        messageHolderType,
        separatorDate,
        background,
        invoiceLinesHolderViewState,
        initialHolder,
        null,
        messageSenderInfo = { null },
        accountOwner,
        false,
        previewProvider = { null },
        paidTextAttachmentContentProvider = { null },
        onBindDownloadMedia = {},
        onBindThreadDownloadMedia = {}
    )

    class ThreadHeader(
        message: Message?,
        messageHolderType: MessageHolderType,
        val chat: Chat,
        val tribeAdmin: Contact?,
        initialHolder: InitialHolderViewState,
        val messageSenderInfo: (Message) -> Triple<PhotoUrl?, ContactAlias?, String>?,
        val accountOwner: () -> Contact,
        val timestamp: String,
        val isExpanded: Boolean = false
    ) : MessageHolderViewState(
        message,
        chat,
        tribeAdmin,
        messageHolderType,
        null,
        BubbleBackground.Gone(false),
        InvoiceLinesHolderViewState(false, false),
        initialHolder,
        null,
        messageSenderInfo,
        accountOwner,
        false,
        { null },
        { null },
        {},
        {}
    ) {
        fun copy(isExpanded: Boolean = this.isExpanded): ThreadHeader {
            return ThreadHeader(
                message,
                messageHolderType,
                chat,
                tribeAdmin,
                initialHolder,
                messageSenderInfo,
                accountOwner,
                timestamp,
                isExpanded
            )
        }
    }

    data class FileAttachment(
        val fileName: FileName?,
        val fileSize: FileSize,
        val isPdf: Boolean,
        val pageCount: Int
    )

}
