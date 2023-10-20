package chat.sphinx.chat_common.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.CallSuper
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavArgs
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.chat_common.BuildConfig
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.model.*
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.attachment.AttachmentFullscreenViewState
import chat.sphinx.chat_common.ui.viewstate.attachment.AttachmentSendViewState
import chat.sphinx.chat_common.ui.viewstate.footer.FooterViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderViewState
import chat.sphinx.chat_common.ui.viewstate.mentions.MessageMentionsViewState
import chat.sphinx.chat_common.ui.viewstate.menu.ChatMenuViewState
import chat.sphinx.chat_common.ui.viewstate.messageholder.*
import chat.sphinx.chat_common.ui.viewstate.messagereply.MessageReplyViewState
import chat.sphinx.chat_common.ui.viewstate.scrolldown.ScrollDownViewState
import chat.sphinx.chat_common.ui.viewstate.search.MessagesSearchViewState
import chat.sphinx.chat_common.ui.viewstate.selected.SelectedMessageViewState
import chat.sphinx.chat_common.ui.viewstate.shimmer.ShimmerViewState
import chat.sphinx.chat_common.ui.viewstate.thread.ThreadHeaderViewState
import chat.sphinx.chat_common.util.AudioPlayerController
import chat.sphinx.chat_common.util.AudioPlayerControllerImpl
import chat.sphinx.chat_common.util.AudioRecorderController
import chat.sphinx.chat_common.util.SphinxLinkify
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_link_preview.LinkPreviewHandler
import chat.sphinx.concept_link_preview.model.TribePreviewName
import chat.sphinx.concept_link_preview.model.toPreviewImageUrlOrNull
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.*
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.feed.FeedItemLink
import chat.sphinx.wrapper_common.feed.toFeedItemLink
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.message.*
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.isNewsletter
import chat.sphinx.wrapper_feed.isPodcast
import chat.sphinx.wrapper_feed.isVideo
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.*
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.GPHSettings
import com.giphy.sdk.ui.themes.GPHTheme
import com.giphy.sdk.ui.themes.GridType
import com.giphy.sdk.ui.utils.aspectRatio
import com.giphy.sdk.ui.views.GiphyDialogFragment
import com.squareup.moshi.Moshi
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import java.io.*


@JvmSynthetic
@Suppress("NOTHING_TO_INLINE")
internal inline fun <ARGS : NavArgs> ChatViewModel<ARGS>.isMessageSelected(): Boolean =
    getSelectedMessageViewStateFlow().value is SelectedMessageViewState.SelectedMessage

abstract class ChatViewModel<ARGS : NavArgs>(
    protected val app: Application,
    dispatchers: CoroutineDispatchers,
    val memeServerTokenHandler: MemeServerTokenHandler,
    val chatNavigator: ChatNavigator,
    protected val repositoryMedia: RepositoryMedia,
    protected val feedRepository: FeedRepository,
    protected val chatRepository: ChatRepository,
    protected val contactRepository: ContactRepository,
    protected val messageRepository: MessageRepository,
    protected val actionsRepository: ActionsRepository,
    protected val repositoryDashboard: RepositoryDashboardAndroid<Any>,
    protected val networkQueryLightning: NetworkQueryLightning,
    protected val networkQueryPeople: NetworkQueryPeople,
    val mediaCacheHandler: MediaCacheHandler,
    protected val savedStateHandle: SavedStateHandle,
    protected val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    protected val linkPreviewHandler: LinkPreviewHandler,
    private val memeInputStreamHandler: MemeInputStreamHandler,
    val moshi: Moshi,
    protected val LOG: SphinxLogger,
) : MotionLayoutViewModel<
        Nothing,
        ChatSideEffectFragment,
        ChatSideEffect,
        ChatMenuViewState,
        >(dispatchers, ChatMenuViewState.Closed) {
    companion object {
        const val TAG = "ChatViewModel"
        const val CONFIG_PLACE_HOLDER = "PLACE_HOLDER"
    }

    protected abstract val args: ARGS
    protected abstract val chatId: ChatId?
    protected abstract val contactId: ContactId?

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .build()
    }

    val messagesSearchViewStateContainer: ViewStateContainer<MessagesSearchViewState> by lazy {
        ViewStateContainer(MessagesSearchViewState.Idle)
    }

    val messageReplyViewStateContainer: ViewStateContainer<MessageReplyViewState> by lazy {
        ViewStateContainer(MessageReplyViewState.ReplyingDismissed)
    }

    val callMenuHandler: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }

    val moreOptionsMenuHandler: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }

    val messageMentionsViewStateContainer: ViewStateContainer<MessageMentionsViewState> by lazy {
        ViewStateContainer(MessageMentionsViewState.MessageMentions(listOf()))
    }

    val scrollDownViewStateContainer: ViewStateContainer<ScrollDownViewState> by lazy {
        ViewStateContainer(ScrollDownViewState.Off)
    }

    val chatHeaderViewStateContainer: ViewStateContainer<ChatHeaderViewState> by lazy {
        ChatHeaderViewStateContainer()
    }

    val threadHeaderViewState: ViewStateContainer<ThreadHeaderViewState> by lazy {
        ViewStateContainer(
            if (isThreadChat()) {
                ThreadHeaderViewState.BasicHeader
            } else {
                ThreadHeaderViewState.Idle
            }
        )
    }

    val shimmerViewState: ViewStateContainer<ShimmerViewState> by lazy {
        ViewStateContainer(ShimmerViewState.On)
    }

    private val latestThreadMessagesFlow: MutableStateFlow<List<Message>?> = MutableStateFlow(null)
    private val scrollDownButtonCount: MutableStateFlow<Long?> = MutableStateFlow(null)

    protected abstract val chatSharedFlow: SharedFlow<Chat?>

    protected abstract val threadSharedFlow: SharedFlow<List<Message>>?

    abstract val headerInitialHolderSharedFlow: SharedFlow<InitialHolderViewState>

    protected abstract suspend fun getChatInfo(): Triple<ChatName?, PhotoUrl?, String>?

    abstract suspend fun shouldStreamSatsFor(podcastClip: PodcastClip, messageUUID: MessageUUID?)

    private inner class ChatHeaderViewStateContainer :
        ViewStateContainer<ChatHeaderViewState>(ChatHeaderViewState.Idle) {

        private var contactCollectionJob: Job? = null
        private var chatCollectionJob: Job? = null

        override val viewStateFlow: StateFlow<ChatHeaderViewState> = flow {

            contactId?.let { nnContactId ->
                contactCollectionJob = viewModelScope.launch(mainImmediate) {
                    // Ensure that chat collection sets state first before collecting the contact
                    // as we must have present the current value for mute
                    while (isActive && _viewStateFlow.value is ChatHeaderViewState.Idle) {
                        delay(25L)
                    }

                    contactRepository.getContactById(nnContactId).collect { contact ->
                        val currentState = _viewStateFlow.value
                        if (contact != null && currentState is ChatHeaderViewState.Initialized) {
                            _viewStateFlow.value = ChatHeaderViewState.Initialized(
                                chatHeaderName = contact.alias?.value ?: "",
                                showLock = currentState.showLock || contact.isEncrypted(),
                                isMuted = currentState.isMuted,
                            )
                        }
                    }
                }
            }

            chatCollectionJob = viewModelScope.launch {
                chatSharedFlow.collect { chat ->

                    _viewStateFlow.value = ChatHeaderViewState.Initialized(
                        chatHeaderName = chat?.name?.value ?: getChatInfo()?.first?.value ?: "",
                        showLock = chat != null,
                        isMuted = chat?.notify?.isMuteChat() == true,
                    )
                    chat?.let { nnChat ->
                        if (nnChat.isPrivateTribe()) {
                            handleDisabledFooterState(nnChat)
                        }
                    }
                }
            }

            emitAll(_viewStateFlow)
        }.onCompletion {
            contactCollectionJob?.cancel()
            chatCollectionJob?.cancel()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ChatHeaderViewState.Idle
        )
    }

    private fun collectThread() {
        viewModelScope.launch(mainImmediate) {
            threadSharedFlow?.collect { messages ->
                latestThreadMessagesFlow.value = messages
            }
        }
    }

    private fun collectUnseenMessagesNumber() {
        viewModelScope.launch(mainImmediate) {
            if (!isThreadChat()) {
                repositoryDashboard.getUnseenMessagesByChatId(getChat().id).collect { unseenMessagesCount ->
                    scrollDownButtonCount.value = unseenMessagesCount
                }
            }
        }
    }

    fun updateScrollDownButton(showButton: Boolean) {
        val newState = if (showButton) {
            val count = scrollDownButtonCount.value?.takeIf { it > 0 }?.toString()
            ScrollDownViewState.On(count)
        } else {
            ScrollDownViewState.Off
        }
        scrollDownViewStateContainer.updateViewState(newState)
    }

    suspend fun getChat(): Chat {
        chatSharedFlow.replayCache.firstOrNull()?.let { chat ->
            return chat
        }

        chatSharedFlow.firstOrNull()?.let { chat ->
            return chat
        }

        var chat: Chat? = null

        try {
            chatSharedFlow.collect {
                if (it != null) {
                    chat = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {
        }
        delay(25L)

        return chat!!
    }

    private fun handleDisabledFooterState(chat: Chat) {
        if (chat.status.isPending()) {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    ChatSideEffect.Notify(
                        app.getString(R.string.waiting_for_admin_approval),
                        false
                    )
                )
            }
        }

        if (!chat.status.isApproved()) {
            footerViewStateContainer.updateViewState(
                FooterViewState.Disabled
            )
        } else if (chat.status.isApproved() && footerViewStateContainer.value == FooterViewState.Disabled) {
            footerViewStateContainer.updateViewState(
                FooterViewState.Default
            )
        }
    }

    abstract suspend fun getInitialHolderViewStateForReceivedMessage(
        message: Message,
        owner: Contact
    ): InitialHolderViewState

    suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
    }

    private fun getBubbleBackgroundForMessage(
        message: Message,
        previousMessage: Message?,
        nextMessage: Message?,
        groupingDate: DateTime?,
    ): Pair<DateTime?, BubbleBackground> {

        val groupingMinutesLimit = 5.0
        var date = groupingDate ?: message.date

        val shouldAvoidGroupingWithPrevious =
            (previousMessage?.shouldAvoidGrouping() ?: true) || message.shouldAvoidGrouping()
        val isGroupedBySenderWithPrevious =
            previousMessage?.hasSameSenderThanMessage(message) ?: false
        val isGroupedByDateWithPrevious =
            message.date.getMinutesDifferenceWithDateTime(date) < groupingMinutesLimit

        val groupedWithPrevious =
            (!shouldAvoidGroupingWithPrevious && isGroupedBySenderWithPrevious && isGroupedByDateWithPrevious)

        date = if (groupedWithPrevious) date else message.date

        val shouldAvoidGroupingWithNext =
            (nextMessage?.shouldAvoidGrouping() ?: true) || message.shouldAvoidGrouping()
        val isGroupedBySenderWithNext = nextMessage?.hasSameSenderThanMessage(message) ?: false
        val isGroupedByDateWithNext =
            if (nextMessage != null) nextMessage.date.getMinutesDifferenceWithDateTime(date) < groupingMinutesLimit else false

        val groupedWithNext =
            (!shouldAvoidGroupingWithNext && isGroupedBySenderWithNext && isGroupedByDateWithNext)

        when {
            (!groupedWithPrevious && !groupedWithNext) -> {
                return Pair(date, BubbleBackground.First.Isolated)
            }
            (groupedWithPrevious && !groupedWithNext) -> {
                return Pair(date, BubbleBackground.Last)
            }
            (!groupedWithPrevious && groupedWithNext) -> {
                return Pair(date, BubbleBackground.First.Grouped)
            }
            (groupedWithPrevious && groupedWithNext) -> {
                return Pair(date, BubbleBackground.Middle)
            }
        }

        return Pair(date, BubbleBackground.First.Isolated)
    }

    private suspend fun getMessageHolderViewStateList(messages: List<Message>): List<MessageHolderViewState> {
        val chat = getChat()

        val chatInfo = getChatInfo()
        val chatName = chatInfo?.first
        val chatPhotoUrl = chatInfo?.second
        val chatColorKey = chatInfo?.third ?: app.getRandomHexCode()

        val owner = getOwner()

        val tribeAdmin = chat.ownerPubKey?.let {
            contactRepository.getContactByPubKey(it).firstOrNull()
        } ?: null

        var unseenSeparatorAdded = false

        val newList = ArrayList<MessageHolderViewState>(messages.size)

        withContext(io) {

            var groupingDate: DateTime? = null
            var openSentPaidInvoicesCount = 0
            var openReceivedPaidInvoicesCount = 0

            val messagesList = filterAndSortMessagesIfNecessary(chat, messages)

            for ((index, message) in messagesList.withIndex()) {

                val previousMessage: Message? = if (index > 0) messagesList[index - 1] else null
                val nextMessage: Message? =
                    if (index < messagesList.size - 1) messagesList[index + 1] else null

                val groupingDateAndBubbleBackground = getBubbleBackgroundForMessage(
                    message,
                    previousMessage,
                    nextMessage,
                    groupingDate
                )

                groupingDate = groupingDateAndBubbleBackground.first

                val sent = message.sender == chat.contactIds.firstOrNull()

                if (message.type.isInvoicePayment()) {
                    if (sent) {
                        openReceivedPaidInvoicesCount -= 1
                    } else {
                        openSentPaidInvoicesCount -= 1
                    }
                }

                val invoiceLinesHolderViewState = InvoiceLinesHolderViewState(
                    openSentPaidInvoicesCount > 0,
                    openReceivedPaidInvoicesCount > 0
                )

                val isOwner: Boolean = message.sender == owner.id

                val isThreadHeaderMessage = (message.uuid?.value == getThreadUUID()?.value && index == 0 && !message.type.isGroupAction())

                if (isThreadHeaderMessage) {
                    newList.add(
                        MessageHolderViewState.ThreadHeader(
                            message,
                            MessageHolderType.ThreadHeader,
                            chat,
                            tribeAdmin,
                            InitialHolderViewState.None,
                            messageSenderInfo = { messageCallback ->
                                when {
                                    messageCallback.sender == chat.contactIds.firstOrNull() -> {
                                        val accountOwner = contactRepository.accountOwner.value

                                        Triple(
                                            accountOwner?.photoUrl,
                                            accountOwner?.alias,
                                            accountOwner?.getColorKey() ?: ""
                                        )
                                    }

                                    chat.type.isConversation() -> {
                                        Triple(
                                            chatPhotoUrl,
                                            chatName?.value?.toContactAlias(),
                                            chatColorKey
                                        )
                                    }

                                    else -> {
                                        Triple(
                                            messageCallback.senderPic,
                                            messageCallback.senderAlias?.value?.toContactAlias(),
                                            messageCallback.getColorKey()
                                        )
                                    }
                                }
                            },
                            accountOwner = { owner },
                            message.date.chatTimeFormat()
                        )
                    )
                    continue
                }

                if (!message.seen.isTrue() && !sent && !unseenSeparatorAdded) {
                    newList.add(
                        MessageHolderViewState.Separator(
                            MessageHolderType.UnseenSeparator,
                            null,
                            chat,
                            tribeAdmin,
                            BubbleBackground.Gone(setSpacingEqual = true),
                            invoiceLinesHolderViewState,
                            InitialHolderViewState.None,
                            accountOwner = { owner }
                        )
                    )
                    unseenSeparatorAdded = true
                }
                // Consider last reply or message and last reply of previous message if exists
                val actualMessage = message.thread?.last() ?: message
                val actualPreviousMessage = previousMessage?.thread?.last() ?: previousMessage

                if (actualPreviousMessage == null || actualMessage.date.isDifferentDayThan(actualPreviousMessage.date)) {
                    newList.add(
                        MessageHolderViewState.Separator(
                            MessageHolderType.DateSeparator,
                            message.date,
                            chat,
                            tribeAdmin,
                            BubbleBackground.Gone(setSpacingEqual = true),
                            invoiceLinesHolderViewState,
                            InitialHolderViewState.None,
                            accountOwner = { owner }
                        )
                    )
                }

                val isDeleted = message.status.isDeleted()

                if (
                    (sent && !message.isPaidInvoice) ||
                    (!sent && message.isPaidInvoice)
                ) {
                    newList.add(
                        MessageHolderViewState.Sent(
                            message,

                            chat,
                            tribeAdmin,
                            background = when {
                                isDeleted -> {
                                    BubbleBackground.Gone(setSpacingEqual = false)
                                }
                                message.type.isInvoicePayment() -> {
                                    BubbleBackground.Gone(setSpacingEqual = false)
                                }
                                message.type.isGroupAction() -> {
                                    BubbleBackground.Gone(setSpacingEqual = true)
                                }
                                else -> {
                                    groupingDateAndBubbleBackground.second
                                }
                            },
                            invoiceLinesHolderViewState = invoiceLinesHolderViewState,
                            highlightedText = null,
                            messageSenderInfo = { messageCallback ->
                                when {
                                    messageCallback.sender == chat.contactIds.firstOrNull() -> {
                                        val accountOwner = contactRepository.accountOwner.value

                                        Triple(
                                            accountOwner?.photoUrl,
                                            accountOwner?.alias,
                                            accountOwner?.getColorKey() ?: ""
                                        )
                                    }
                                    chat.type.isConversation() -> {
                                        Triple(
                                            chatPhotoUrl,
                                            chatName?.value?.toContactAlias(),
                                            chatColorKey
                                        )
                                    }
                                    else -> {
                                        Triple(
                                            messageCallback.senderPic,
                                            messageCallback.senderAlias?.value?.toContactAlias(),
                                            messageCallback.getColorKey()
                                        )
                                    }
                                }
                            },
                            accountOwner = { owner },
                            urlLinkPreviewsEnabled = areUrlLinkPreviewsEnabled(),
                            previewProvider = { handleLinkPreview(it) },
                            paidTextMessageContentProvider = { messageCallback ->
                                handlePaidTextMessageContent(messageCallback)
                            },
                            onBindDownloadMedia = {
                                repositoryMedia.downloadMediaIfApplicable(message, sent)
                            },
                            onBindThreadDownloadMedia = {
                                message.thread?.last()?.let { lastReplyMessage ->
                                    repositoryMedia.downloadMediaIfApplicable(lastReplyMessage, sent)
                                }
                            }
                        )
                    )
                } else {
                        newList.add(
                            MessageHolderViewState.Received(
                                message,
                                chat,
                                tribeAdmin,
                                background = when {
                                    isDeleted -> {
                                        BubbleBackground.Gone(setSpacingEqual = false)
                                    }

                                    message.isFlagged -> {
                                        BubbleBackground.Gone(setSpacingEqual = false)
                                    }

                                    message.type.isInvoicePayment() -> {
                                        BubbleBackground.Gone(setSpacingEqual = false)
                                    }

                                    message.type.isGroupAction() -> {
                                        BubbleBackground.Gone(setSpacingEqual = true)
                                    }

                                    else -> {
                                        groupingDateAndBubbleBackground.second
                                    }
                                },
                                invoiceLinesHolderViewState = invoiceLinesHolderViewState,
                                initialHolder = when {
                                    isDeleted || message.type.isGroupAction() -> {
                                        InitialHolderViewState.None
                                    }

                                    else -> {
                                        getInitialHolderViewStateForReceivedMessage(message, owner)
                                    }
                                },
                                highlightedText = null,
                                messageSenderInfo = { messageCallback ->
                                    when {
                                        messageCallback.sender == chat.contactIds.firstOrNull() -> {
                                            val accountOwner = contactRepository.accountOwner.value

                                            Triple(
                                                accountOwner?.photoUrl,
                                                accountOwner?.alias,
                                                accountOwner?.getColorKey() ?: ""
                                            )
                                        }

                                        chat.type.isConversation() -> {
                                            Triple(
                                                chatPhotoUrl,
                                                chatName?.value?.toContactAlias(),
                                                chatColorKey
                                            )
                                        }

                                        else -> {
                                            Triple(
                                                messageCallback.senderPic,
                                                messageCallback.senderAlias?.value?.toContactAlias(),
                                                messageCallback.getColorKey()
                                            )
                                        }
                                    }
                                },
                                accountOwner = { owner },
                                urlLinkPreviewsEnabled = areUrlLinkPreviewsEnabled(),
                                previewProvider = { link -> handleLinkPreview(link) },
                                paidTextMessageContentProvider = { messageCallback ->
                                    handlePaidTextMessageContent(messageCallback)
                                },
                                onBindDownloadMedia = {
                                    repositoryMedia.downloadMediaIfApplicable(message, sent)
                                },
                                onBindThreadDownloadMedia = {
                                    message.thread?.last()?.let { lastReplyMessage ->
                                        repositoryMedia.downloadMediaIfApplicable(lastReplyMessage, sent)
                                    }
                                }
                            )
                        )
                }

                if (message.isPaidInvoice) {
                    if (sent) {
                        openSentPaidInvoicesCount += 1
                    } else {
                        openReceivedPaidInvoicesCount += 1
                    }
                }
            }
        }

        return newList
    }

    private fun filterAndSortMessagesIfNecessary(
        chat: Chat,
        messages: List<Message>,
    ): List<Message> {
        val filteredMessages: MutableList<Message> = mutableListOf()
        val threadMessageMap: MutableMap<String, Int> = mutableMapOf()

        // Filter messages to do not show thread replies on chat
        if (chat.isTribe() && !isThreadChat()) {
            for (message in messages) {

                if (message.thread?.isNotEmpty() == true) {
                    message.uuid?.value?.let { uuid ->
                        threadMessageMap[uuid] = message.thread?.count() ?: 0
                    }
                }

                val shouldAddMessage = message.threadUUID?.let { threadUUID ->
                    val count = threadMessageMap[threadUUID.value] ?: 0
                    count <= 1
                } ?: true

                if (shouldAddMessage) {
                    filteredMessages.add(message)
                }
            }
        } else {
            filteredMessages.addAll(messages)
        }

        // Sort messages list by the last thread message date if applicable

        return filteredMessages.sortedBy { it.thread?.last()?.date?.value ?: it.date.value }
    }

    internal val messageHolderViewStateFlow: MutableStateFlow<List<MessageHolderViewState>> by lazy {
        MutableStateFlow(listOf())
    }

    private suspend fun handleLinkPreview(link: MessageLinkPreview): LayoutState.Bubble.ContainerThird.LinkPreview? {
        var preview: LayoutState.Bubble.ContainerThird.LinkPreview? = null

        viewModelScope.launch(mainImmediate) {
            // TODO: Implement
            @Exhaustive
            when (link) {
                is NodeDescriptor -> {

                    val pubKey: LightningNodePubKey? = when (link.nodeDescriptor) {
                        is LightningNodePubKey -> {
                            link.nodeDescriptor
                        }
                        is VirtualLightningNodeAddress -> {
                            link.nodeDescriptor.getPubKey()
                        }
                    }

                    if (pubKey != null) {
                        val existingContact: Contact? =
                            contactRepository.getContactByPubKey(pubKey).firstOrNull()

                        if (existingContact != null) {

                            preview = LayoutState.Bubble.ContainerThird.LinkPreview.ContactPreview(
                                alias = existingContact.alias,
                                photoUrl = existingContact.photoUrl,
                                showBanner = false,
                                lightningNodeDescriptor = link.nodeDescriptor,
                            )

                        } else {

                            preview = LayoutState.Bubble.ContainerThird.LinkPreview.ContactPreview(
                                alias = null,
                                photoUrl = null,
                                showBanner = true,
                                lightningNodeDescriptor = link.nodeDescriptor
                            )

                        }
                    }

                }
                is TribeLink -> {
                    try {
                        val uuid = ChatUUID(link.tribeJoinLink.tribeUUID)

                        val thisChat = getChat()
                        if (thisChat.uuid == uuid) {

                            preview =
                                LayoutState.Bubble.ContainerThird.LinkPreview.TribeLinkPreview(
                                    name = TribePreviewName(thisChat.name?.value ?: ""),
                                    description = null,
                                    imageUrl = thisChat.photoUrl?.toPreviewImageUrlOrNull(),
                                    showBanner = true,
                                    joinLink = link.tribeJoinLink,
                                )

                        } else {
                            val existingChat = chatRepository.getChatByUUID(uuid).firstOrNull()
                            if (existingChat != null) {

                                preview =
                                    LayoutState.Bubble.ContainerThird.LinkPreview.TribeLinkPreview(
                                        name = TribePreviewName(existingChat.name?.value ?: ""),
                                        description = null,
                                        imageUrl = existingChat.photoUrl?.toPreviewImageUrlOrNull(),
                                        showBanner = false,
                                        joinLink = link.tribeJoinLink,
                                    )

                            } else {

                                val tribePreview =
                                    linkPreviewHandler.retrieveTribeLinkPreview(link.tribeJoinLink)

                                if (tribePreview != null) {
                                    preview =
                                        LayoutState.Bubble.ContainerThird.LinkPreview.TribeLinkPreview(
                                            name = tribePreview.name,
                                            description = tribePreview.description,
                                            imageUrl = tribePreview.imageUrl,
                                            showBanner = true,
                                            joinLink = link.tribeJoinLink,
                                        )
                                } // else do nothing
                            }
                        }
                    } catch (_: Exception) {
                        // no - op
                    }
                }
                is FeedItemPreview -> {}
                is UnspecifiedUrl -> {

                    if (areUrlLinkPreviewsEnabled()) {
                        val htmlPreview = linkPreviewHandler.retrieveHtmlPreview(link.url)

                        if (htmlPreview != null) {
                            preview = LayoutState.Bubble.ContainerThird.LinkPreview.HttpUrlPreview(
                                title = htmlPreview.title,
                                domainHost = htmlPreview.domainHost,
                                description = htmlPreview.description,
                                imageUrl = htmlPreview.imageUrl,
                                favIconUrl = htmlPreview.favIconUrl,
                                url = link.url
                            )
                        }
                    }
                }
            }
        }.join()

        return preview
    }

    var urlLinkPreviewsEnabled: Boolean? = null
    private fun areUrlLinkPreviewsEnabled(): Boolean {
        urlLinkPreviewsEnabled?.let {
            return it
        }

        val appContext: Context = app.applicationContext
        val serverUrlsSharedPreferences = appContext.getSharedPreferences(
            PreviewsEnabled.LINK_PREVIEWS_SHARED_PREFERENCES,
            Context.MODE_PRIVATE
        )

        urlLinkPreviewsEnabled = serverUrlsSharedPreferences.getBoolean(
            PreviewsEnabled.LINK_PREVIEWS_ENABLED_KEY,
            PreviewsEnabled.True.isTrue()
        )

        return urlLinkPreviewsEnabled!!
    }

    private suspend fun handlePaidTextMessageContent(message: Message): LayoutState.Bubble.ContainerThird.Message? {
        var messageLayoutState: LayoutState.Bubble.ContainerThird.Message? = null

        viewModelScope.launch(mainImmediate) {
            message.retrievePaidTextAttachmentUrlAndMessageMedia()?.let { urlAndMedia ->
                urlAndMedia.second?.host?.let { host ->
                    urlAndMedia.second?.mediaKeyDecrypted?.let { mediaKeyDecrypted ->
                        memeServerTokenHandler.retrieveAuthenticationToken(host)?.let { token ->

                            val streamAndFileName = memeInputStreamHandler.retrieveMediaInputStream(
                                urlAndMedia.first,
                                token,
                                mediaKeyDecrypted
                            )

                            var text: String? = null

                            viewModelScope.launch(io) {
                                text = streamAndFileName?.first?.bufferedReader()
                                    .use { it?.readText() }
                            }.join()

                            text?.let { nnText ->
                                messageLayoutState = LayoutState.Bubble.ContainerThird.Message(
                                    text = nnText,
                                    decryptionError = false,
                                    isThread = false
                                )

                                nnText.toMessageContentDecrypted()?.let { messageContentDecrypted ->
                                    messageRepository.updateMessageContentDecrypted(
                                        message.id,
                                        messageContentDecrypted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return messageLayoutState
    }

    fun init() {
        // Prime our states immediately so they're already
        // updated by the time the Fragment's onStart is called
        // where they're collected.
        val setupChatFlowJob = viewModelScope.launch(mainImmediate) {
            chatSharedFlow.firstOrNull()
        }
        val setupHeaderInitialHolderJob = viewModelScope.launch(mainImmediate) {
            headerInitialHolderSharedFlow.firstOrNull()
        }
        val setupViewStateContainerJob = viewModelScope.launch(mainImmediate) {
            viewStateContainer.viewStateFlow.firstOrNull()
        }
        forceKeyExchange()

        viewModelScope.launch(mainImmediate) {
            delay(500)
            // cancel the setup jobs as the view has taken over observation
            // and we don't want to continue collecting endlessly if any of
            // them are still active. WhileSubscribed will take over.
            setupChatFlowJob.cancel()
            setupHeaderInitialHolderJob.cancel()
            setupViewStateContainerJob.cancel()
        }

        audioPlayerController.streamSatsHandler = { messageUUID, podcastClip ->
            podcastClip?.let { nnPodcastClip ->
                viewModelScope.launch(io) {
                    shouldStreamSatsFor(nnPodcastClip, messageUUID)
                }
            }
        }
    }

    protected abstract fun forceKeyExchange()

    var messagesLoadJob: Job? = null
    fun screenInit() {
        if (messageHolderViewStateFlow.value.isNotEmpty()) {
            return
        }

        if (chatId == null) {
            shimmerViewState.updateViewState(ShimmerViewState.Off)
        }

        var isScrollDownButtonSetup = false
        messagesLoadJob = viewModelScope.launch(mainImmediate) {
            if (isThreadChat()) {
                messageRepository.getAllMessagesToShowByChatId(getChat().id, 0, getThreadUUID()).distinctUntilChanged().collect { messages ->
                    delay(200)

                    val originalMessage = messageRepository.getMessageByUUID(MessageUUID(getThreadUUID()?.value!!)).firstOrNull()
                    val completeThread = listOf(originalMessage) + messages.reversed()
                    val list = getMessageHolderViewStateList(completeThread.filterNotNull()).toList()

                    messageHolderViewStateFlow.value = list
                    changeThreadHeaderState(true)
                    shimmerViewState.updateViewState(ShimmerViewState.Off)

                    scrollDownButtonCount.value = messages.size.toLong()
                }
            } else {
                messageRepository.getAllMessagesToShowByChatId(getChat().id, 100).firstOrNull()?.let { messages ->
                    delay(200)

                    messageHolderViewStateFlow.value = getMessageHolderViewStateList(messages).toList()
                    shimmerViewState.updateViewState(ShimmerViewState.Off)
                }

                delay(1000L)

                messageRepository.getAllMessagesToShowByChatId(getChat().id, 1000).distinctUntilChanged().collect { messages ->
                    messageHolderViewStateFlow.value = getMessageHolderViewStateList(messages).toList()
                    shimmerViewState.updateViewState(ShimmerViewState.Off)

                    reloadPinnedMessage()

                    if (!isScrollDownButtonSetup) {
                        setupScrollDownButtonCount()
                        isScrollDownButtonSetup = true
                    }
                }
            }
        }
        collectThread()
        collectUnseenMessagesNumber()
    }

        private fun setupScrollDownButtonCount() {
        val unseenMessagesCount = scrollDownButtonCount.value ?: 0
        if (unseenMessagesCount > 0.toLong()) {
            scrollDownViewStateContainer.updateViewState(
                ScrollDownViewState.On(unseenMessagesCount.toString())
            )
        } else {
            scrollDownViewStateContainer.updateViewState(
                ScrollDownViewState.Off
            )
        }
    }

    abstract val checkRoute: Flow<LoadResponse<Boolean, ResponseError>>

    abstract fun readMessages()

    abstract fun reloadPinnedMessage()

    abstract fun getThreadUUID(): ThreadUUID?

    abstract fun isThreadChat(): Boolean

    suspend fun createPaidMessageFile(text: String?): File? {
        if (text.isNullOrEmpty()) {
            return null
        }

        return try {
            val output = mediaCacheHandler.createPaidTextFile("txt")
            mediaCacheHandler.copyTo(text.byteInputStream(), output)
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Builds the [SendMessage] and returns it (or null if it was invalid),
     * then passes it off to the [MessageRepository] for processing.
     * */
    /**
     * Builds the [SendMessage] and returns it (or null if it was invalid),
     * then passes it off to the [MessageRepository] for processing.
     * */
    @CallSuper
    open suspend fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        val msg = builder.build()

        msg.second?.let { validationError ->
            val errorMessageRes = when (validationError) {
                SendMessage.Builder.ValidationError.EMPTY_PRICE -> {
                    R.string.send_message_empty_price_error
                }
                SendMessage.Builder.ValidationError.EMPTY_CONTENT -> {
                    R.string.send_message_empty_content_error
                }
                SendMessage.Builder.ValidationError.EMPTY_DESTINATION -> {
                    R.string.send_message_empty_destination_error
                }
            }

            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    ChatSideEffect.Notify(
                        app.getString(errorMessageRes)
                    )
                )
            }

        } ?: msg.first?.let { message ->
            messageRepository.sendMessage(message)

            joinCallIfNeeded(message)
//            trackMessage(message.text)
        }

        return msg.first
    }

    private fun joinCallIfNeeded(message: SendMessage) {
        if (message.isCall) {
            message.text
                ?.replaceFirst(CallLinkMessage.MESSAGE_PREFIX, "")
                ?.toCallLinkMessageOrNull(moshi)?.link?.let { link ->
                    joinCall(link, link.startAudioOnly)
            }
        }
    }

//    private fun trackMessage(text: String?) {
//        viewModelScope.launch(io) {
//            if (text.isNullOrEmpty()) {
//                return@launch
//            }
//
//            val keywordList = extractKeywords(text)
//            keywordList?.let { list ->
//                actionsRepository.trackMessageContent(list)
//            }
//        }
//    }

//    private fun extractKeywords(text: String): List<String>? {
//        val pyObj = python.getModule("keyword_extractor")
//        val obj = pyObj.callAttr("extract_keywords", text)
//
//        val keywords = obj.asList().map {
//            it.toString().substringAfter("(\'").substringBefore("',")
//        }
//
//        return keywords.take(5)
//    }


    /**
     * Remotely and locally Deletes a [Message] through the [MessageRepository]
     */
    /**
     * Remotely and locally Deletes a [Message] through the [MessageRepository]
     */
    open fun deleteMessage(message: Message) {
        val sideEffect = ChatSideEffect.AlertConfirmDeleteMessage {
            viewModelScope.launch(mainImmediate) {
                when (messageRepository.deleteMessage(message)) {
                    is Response.Error -> {
                        submitSideEffect(ChatSideEffect.Notify("Failed to delete Message"))
                    }
                    is Response.Success -> {}
                }
            }
        }

        viewModelScope.launch(mainImmediate) {
            submitSideEffect(sideEffect)
        }
    }

    private var toggleChatMutedJob: Job? = null
    fun toggleChatMuted() {
        chatSharedFlow.replayCache.firstOrNull()?.let { chat ->

            if (chat.isTribe()) {
                navigateToNotificationLevel()
                return@let
            }

            if (toggleChatMutedJob?.isActive == true) {
                return
            }

            toggleChatMutedJob = viewModelScope.launch(mainImmediate) {

                submitSideEffect(ChatSideEffect.ProduceHapticFeedback)

                val newLevel =
                    if (chat.notify?.isMuteChat() == true) NotificationLevel.SeeAll else NotificationLevel.MuteChat
                val response = chatRepository.setNotificationLevel(chat, newLevel)

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        submitSideEffect(
                            ChatSideEffect.Notify(response.message)
                        )
                        delay(2_000)
                    }
                    is Response.Success -> {
                        if (response.value) {
                            submitSideEffect(
                                ChatSideEffect.Notify(
                                    app.getString(R.string.chat_muted_message)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    abstract fun navigateToNotificationLevel()

    var messagesSearchJob: Job? = null
    suspend fun searchMessages(text: String?) {
        moreOptionsMenuHandler.updateViewState(
            MenuBottomViewState.Closed
        )

        if (
            messagesSearchViewStateContainer.viewStateFlow.value is MessagesSearchViewState.Idle
        ) {
            loadAllMessages()
        }

        chatId?.let { nnChatId ->
            text?.let { nnText ->
                if (nnText.toCharArray().size > 2) {
                    messagesSearchViewStateContainer.updateViewState(
                        MessagesSearchViewState.Loading(
                            true
                        )
                    )

                    messagesSearchJob?.cancel()
                    messagesSearchJob = viewModelScope.launch(io) {
                        delay(500L)

                        messageRepository.searchMessagesBy(nnChatId, nnText).firstOrNull()
                            ?.let { messages ->
                                messagesSearchViewStateContainer.updateViewState(
                                    MessagesSearchViewState.Searching(
                                        true,
                                        messages,
                                        0,
                                        true
                                    )
                                )
                            }
                    }
                    return
                }
            }
        }

        messagesSearchViewStateContainer.updateViewState(
            MessagesSearchViewState.Searching(
                (text ?: "").isNotEmpty(),
                emptyList(),
                0,
                true
            )
        )
    }

    fun cancelSearch() {
        messagesSearchViewStateContainer.updateViewState(
            MessagesSearchViewState.Cancel
        )
    }

    fun clearSearch() {
        messagesSearchViewStateContainer.updateViewState(
            MessagesSearchViewState.Clear
        )
    }

    fun navigateResults(
        advanceBy: Int
    ) {
        val searchViewState = messagesSearchViewStateContainer.viewStateFlow.value
        if (searchViewState is MessagesSearchViewState.Searching) {
            messagesSearchViewStateContainer.updateViewState(
                MessagesSearchViewState.Searching(
                    searchViewState.clearButtonVisible,
                    searchViewState.messages,
                    searchViewState.index + advanceBy,
                    advanceBy > 0
                )
            )
        }
    }

    private fun loadAllMessages() {
        if (messagesLoadJob?.isActive == true) {
            messagesLoadJob?.cancel()
        }
        messagesLoadJob = viewModelScope.launch(io) {
            messageRepository.getAllMessagesToShowByChatId(getChat().id, 0)
                .distinctUntilChanged().collect { messages ->
                    messageHolderViewStateFlow.value =
                        getMessageHolderViewStateList(messages).toList()
                }
        }
    }

    private val selectedMessageContainer: ViewStateContainer<SelectedMessageViewState> by lazy {
        ViewStateContainer(SelectedMessageViewState.None)
    }

    @JvmSynthetic
    internal fun getSelectedMessageViewStateFlow(): StateFlow<SelectedMessageViewState> =
        selectedMessageContainer.viewStateFlow

    @JvmSynthetic
    internal fun updateSelectedMessageViewState(selectedMessageViewState: SelectedMessageViewState?) {
        if (selectedMessageViewState == null) return

        selectedMessageContainer.updateViewState(selectedMessageViewState)
    }

    private val footerViewStateContainer: ViewStateContainer<FooterViewState> by lazy {
        ViewStateContainer(FooterViewState.Default)
    }

    @JvmSynthetic
    internal fun getFooterViewStateFlow(): StateFlow<FooterViewState> =
        footerViewStateContainer.viewStateFlow

    @JvmSynthetic
    internal fun updateFooterViewState(viewState: FooterViewState) {
        footerViewStateContainer.updateViewState(viewState)
    }

    private inner class AttachmentSendStateContainer :
        ViewStateContainer<AttachmentSendViewState>(AttachmentSendViewState.Idle) {
        override fun updateViewState(viewState: AttachmentSendViewState) {
            if (viewState is AttachmentSendViewState.Preview) {

                // Only delete the previous file in the event that a new pic is chosen
                // to send when one is currently being previewed.
                val current = viewStateFlow.value
                if (current is AttachmentSendViewState.Preview) {
                    if (current.file?.path != viewState.file?.path) {
                        try {
                            current.file?.delete()
                        } catch (e: Exception) {

                        }
                    }
                }
            } else if (viewState is AttachmentSendViewState.PreviewGiphy) {

                // Only delete the previous file in the event that a new pic is chosen
                // to send when one is currently being previewed.
                val current = viewStateFlow.value
                if (current is AttachmentSendViewState.Preview) {
                    try {
                        current.file?.delete()
                    } catch (e: Exception) {

                    }
                }
            }

            super.updateViewState(viewState)
        }
    }

    private val attachmentSendStateContainer: ViewStateContainer<AttachmentSendViewState> by lazy {
        AttachmentSendStateContainer()
    }

    @JvmSynthetic
    internal fun getAttachmentSendViewStateFlow(): StateFlow<AttachmentSendViewState> =
        attachmentSendStateContainer.viewStateFlow

    @JvmSynthetic
    internal fun updateAttachmentSendViewState(viewState: AttachmentSendViewState) {
        attachmentSendStateContainer.updateViewState(viewState)
    }

    @JvmSynthetic
    internal fun deleteUnsentAttachment(viewState: AttachmentSendViewState.Preview) {
        viewModelScope.launch(io) {
            try {
                viewState.file?.delete()
            } catch (e: Exception) {
            }
        }
    }

    private val attachmentFullscreenStateContainer: ViewStateContainer<AttachmentFullscreenViewState> by lazy {
        ViewStateContainer(AttachmentFullscreenViewState.Idle)
    }

    @JvmSynthetic
    internal fun getAttachmentFullscreenViewStateFlow(): StateFlow<AttachmentFullscreenViewState> =
        attachmentFullscreenStateContainer.viewStateFlow

    @JvmSynthetic
    internal fun updateAttachmentFullscreenViewState(viewState: AttachmentFullscreenViewState) {
        if (viewState is AttachmentFullscreenViewState.Idle) {
            val currentState = attachmentFullscreenStateContainer.viewStateFlow.value

            if (currentState is AttachmentFullscreenViewState.PdfFullScreen) {
                currentState.pdfRender.close()
            }
        }
        attachmentFullscreenStateContainer.updateViewState(viewState)
    }

    suspend fun handleCommonChatOnBackPressed() {
        val attachmentSendViewState = getAttachmentSendViewStateFlow().value
        val attachmentFullscreenViewState = getAttachmentFullscreenViewStateFlow().value
        val messageSearchViewState = messagesSearchViewStateContainer.value

        when {
            currentViewState is ChatMenuViewState.Open -> {
                updateViewState(ChatMenuViewState.Closed)
            }
            moreOptionsMenuHandler.value is MenuBottomViewState.Open -> {
                moreOptionsMenuHandler.updateViewState(MenuBottomViewState.Closed)
            }
            callMenuHandler.value is MenuBottomViewState.Open -> {
                callMenuHandler.updateViewState(MenuBottomViewState.Closed)
            }
            attachmentFullscreenViewState is AttachmentFullscreenViewState.ImageFullscreen -> {
                updateAttachmentFullscreenViewState(AttachmentFullscreenViewState.Idle)
            }
            attachmentSendViewState is AttachmentSendViewState.Preview -> {
                updateAttachmentSendViewState(AttachmentSendViewState.Idle)
                updateFooterViewState(FooterViewState.Default)
                deleteUnsentAttachment(attachmentSendViewState)
            }
            attachmentSendViewState is AttachmentSendViewState.PreviewGiphy -> {
                updateAttachmentSendViewState(AttachmentSendViewState.Idle)
                updateFooterViewState(FooterViewState.Default)
            }
            getSelectedMessageViewStateFlow().value is SelectedMessageViewState.SelectedMessage -> {
                updateSelectedMessageViewState(SelectedMessageViewState.None)
            }
            messageSearchViewState is MessagesSearchViewState.Searching -> {
                messagesSearchViewStateContainer.updateViewState(MessagesSearchViewState.Idle)
            }
            messageSearchViewState is MessagesSearchViewState.Loading -> {
                messagesSearchViewStateContainer.updateViewState(MessagesSearchViewState.Idle)
            }
            else -> {
                chatNavigator.popBackStack()
            }
        }
    }

    fun boostMessage(messageUUID: MessageUUID?) {
        if (messageUUID == null) return

        viewModelScope.launch(mainImmediate) {
            val chat = getChat()
            val response = messageRepository.boostMessage(
                chatId = chat.id,
                pricePerMessage = chat.pricePerMessage ?: Sat(0),
                escrowAmount = chat.escrowAmount ?: Sat(0),
                messageUUID = messageUUID,
            )

            @Exhaustive
            when (response) {
                is Response.Error -> {
                    submitSideEffect(
                        ChatSideEffect.Notify(app.getString(R.string.notify_boost_failure))
                    )
                }
                is Response.Success -> {}
            }
        }
    }

    fun toggleThreadDescriptionExpanded() {
        val currentList = messageHolderViewStateFlow.value

        val newList = currentList.map { item ->
            if (item is MessageHolderViewState.ThreadHeader) {
                item.copy(isExpanded = !item.isExpanded)
            } else {
                item
            }
        }
        messageHolderViewStateFlow.value = newList
    }

    fun changeThreadHeaderState(isFullHeader: Boolean){
        if (isFullHeader) {
            (messageHolderViewStateFlow.value.getOrNull(0) as? MessageHolderViewState.ThreadHeader)?.let { viewState ->
                val threadHeader = ThreadHeaderViewState.FullHeader(
                    viewState.messageSenderInfo(viewState.message!!),
                    viewState.timestamp,
                    viewState.bubbleMessage?.text,
                    viewState.bubbleImageAttachment,
                    viewState.bubbleVideoAttachment,
                    viewState.bubbleFileAttachment,
                    viewState.bubbleAudioAttachment
                )
                threadHeaderViewState.updateViewState(threadHeader)
            }
        }
        else {
            threadHeaderViewState.updateViewState(ThreadHeaderViewState.BasicHeader)
        }
    }

    fun copyMessageText(message: Message) {
        viewModelScope.launch(mainImmediate) {
            message.retrieveTextToShow()?.let { text ->
                submitSideEffect(
                    ChatSideEffect.CopyTextToClipboard(text)
                )
            }
        }
    }

    fun copyMessageLink(message: Message) {
        viewModelScope.launch(mainImmediate) {
            message.retrieveTextToShow()?.let { text ->
                val matcher = SphinxLinkify.SphinxPatterns.COPYABLE_LINKS.matcher(text)
                if (matcher.find()) {
                    submitSideEffect(
                        ChatSideEffect.CopyLinkToClipboard(matcher.group())
                    )
                } else {
                    submitSideEffect(
                        ChatSideEffect.Notify(app.getString(R.string.side_effect_no_link_to_copy))
                    )
                }
            }
        }
    }

    fun replyToMessage(message: Message?) {
        if (message != null) {
            viewModelScope.launch(mainImmediate) {
                val chat = getChat()

                val senderAlias = when {
                    message.sender == chat.contactIds.firstOrNull() -> {
                        contactRepository.accountOwner.value?.alias?.value ?: ""
                    }
                    chat.type.isConversation() -> {
                        getChatInfo()?.first?.value ?: ""
                    }
                    else -> {
                        message.senderAlias?.value ?: ""
                    }
                }

                messageReplyViewStateContainer.updateViewState(
                    MessageReplyViewState.ReplyingToMessage(
                        message,
                        senderAlias
                    )
                )
            }
        } else {
            messageReplyViewStateContainer.updateViewState(MessageReplyViewState.ReplyingDismissed)
        }
    }

    fun resendMessage(message: Message) {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()
            messageRepository.resendMessage(message, chat)
        }
    }

    fun flagMessage(message: Message) {
        val sideEffect = ChatSideEffect.AlertConfirmFlagMessage {
            viewModelScope.launch(mainImmediate) {
                val chat = getChat()
                messageRepository.flagMessage(message, chat)
            }
        }

        viewModelScope.launch(mainImmediate) {
            submitSideEffect(sideEffect)
        }
    }

    @JvmSynthetic
    internal fun chatMenuOptionCamera() {
        viewModelScope.launch(mainImmediate) {
            val response = cameraCoordinator.submitRequest(CameraRequest)
            @Exhaustive
            when (response) {
                is Response.Error -> {}
                is Response.Success -> {
                    val ext = response.value.value.extension

                    val mediaType: MediaType = when (response.value) {
                        is CameraResponse.Image -> {
                            MediaType.Image("${MediaType.IMAGE}/$ext")
                        }
                        is CameraResponse.Video -> {
                            MediaType.Video("${MediaType.VIDEO}/$ext")
                        }
                    }

                    updateViewState(ChatMenuViewState.Closed)

                    updateAttachmentSendViewState(
                        AttachmentSendViewState.Preview(response.value.value, mediaType, null, null)
                    )

                    updateFooterViewState(FooterViewState.Attachment)
                }
            }
        }
    }

    @JvmSynthetic
    internal fun chatMenuOptionMediaLibrary() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(ChatSideEffect.RetrieveImageOrVideo)
        }
    }

    @JvmSynthetic
    internal fun chatMenuOptionGif(parentFragmentManager: FragmentManager) {
        if (BuildConfig.GIPHY_API_KEY != CONFIG_PLACE_HOLDER) {
            val settings = GPHSettings(GridType.waterfall, GPHTheme.Dark)
            settings.mediaTypeConfig =
                arrayOf(GPHContentType.gif, GPHContentType.sticker, GPHContentType.recents)

            val giphyDialogFragment =
                GiphyDialogFragment.newInstance(settings, BuildConfig.GIPHY_API_KEY)

            giphyDialogFragment.gifSelectionListener =
                object : GiphyDialogFragment.GifSelectionListener {
                    override fun didSearchTerm(term: String) {}

                    override fun onDismissed(selectedContentType: GPHContentType) {}

                    override fun onGifSelected(
                        media: Media,
                        searchTerm: String?,
                        selectedContentType: GPHContentType
                    ) {
                        updateViewState(ChatMenuViewState.Closed)
                        val giphyData = GiphyData(
                            media.id,
                            "https://media.giphy.com/media/${media.id}/giphy.gif",
                            media.aspectRatio.toDouble(),
                            null
                        )

                        updateAttachmentSendViewState(
                            AttachmentSendViewState.PreviewGiphy(giphyData)
                        )

                        updateFooterViewState(FooterViewState.Attachment)
                    }
                }
            giphyDialogFragment.show(parentFragmentManager, "giphy_search")
        } else {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    ChatSideEffect.Notify("Giphy search not available")
                )
            }
        }
    }

    @JvmSynthetic
    internal val onIMEContent =
        InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
            val lacksPermission =
                (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "Failed to get content from IME", e)

                    viewModelScope.launch(mainImmediate) {
                        submitSideEffect(
                            ChatSideEffect.Notify("Require permission for this content")
                        )
                    }
                    return@OnCommitContentListener false
                }
            }
            handleActivityResultUri(inputContentInfo.contentUri)
            inputContentInfo.releasePermission()
            true
        }

    @JvmSynthetic
    internal fun chatMenuOptionFileLibrary() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(
                ChatSideEffect.RetrieveFile
            )
        }
    }

    @JvmSynthetic
    internal fun chatMenuOptionPaidMessage() {
        updateAttachmentSendViewState(
            AttachmentSendViewState.Preview(null, MediaType.Text, null, null)
        )
        updateViewState(ChatMenuViewState.Closed)
        updateFooterViewState(FooterViewState.Attachment)
    }

    @JvmSynthetic
    internal fun chatMenuOptionPaymentRequest() {
        contactId?.let { id ->
            viewModelScope.launch(mainImmediate) {
                audioPlayerController.pauseMediaIfPlaying()
                chatNavigator.toPaymentReceiveDetail(id, chatId)
            }
            updateViewState(ChatMenuViewState.Closed)
        }
    }

    @JvmSynthetic
    internal fun chatMenuOptionPaymentSend() {
        contactId?.let { id ->
            viewModelScope.launch(mainImmediate) {
                audioPlayerController.pauseMediaIfPlaying()
                chatNavigator.toPaymentSendDetail(id, chatId)
            }
            updateViewState(ChatMenuViewState.Closed)
        }
    }

    private fun openCamera() {
        viewModelScope.launch(mainImmediate) {
            val response = cameraCoordinator.submitRequest(CameraRequest)
            @Exhaustive
            when (response) {
                is Response.Error -> {}
                is Response.Success -> {

                    val ext = response.value.value.extension

                    val mediaType: MediaType = when (response.value) {
                        is CameraResponse.Image -> {
                            MediaType.Image("${MediaType.IMAGE}/$ext")
                        }
                        is CameraResponse.Video -> {
                            MediaType.Image("${MediaType.VIDEO}/$ext")
                        }
                    }

                    updateAttachmentSendViewState(
                        AttachmentSendViewState.Preview(response.value.value, mediaType, null, null)
                    )

                    updateFooterViewState(FooterViewState.Attachment)
                }
            }
        }
    }

    fun handleActivityResultUri(uri: Uri?) {
        if (uri == null) {
            return
        }

        val cr = app.contentResolver

        cr.getType(uri)?.let { crType ->

            MimeTypeMap.getSingleton().getExtensionFromMimeType(crType)?.let { ext ->

                val stream: InputStream = try {
                    cr.openInputStream(uri) ?: return
                } catch (e: Exception) {
                    return
                }

                crType.toMediaType().let { mType ->

                    val newFile: File? = when (mType) {
                        is MediaType.Image -> {
                            mediaCacheHandler.createImageFile(ext)
                        }
                        is MediaType.Video -> {
                            mediaCacheHandler.createVideoFile(ext)
                        }
                        is MediaType.Pdf -> {
                            mediaCacheHandler.createPdfFile(ext)
                        }
                        is MediaType.Text,
                        is MediaType.Unknown -> {
                            mediaCacheHandler.createFile(mType, ext)
                        }
                        else -> {
                            null
                        }
                    }

                    newFile?.let { nnNewFile ->

                        val fileName: String? =
                            cr.query(uri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                cursor.moveToFirst()
                                cursor.getString(nameIndex)
                            } ?: null

                        viewModelScope.launch(mainImmediate) {
                            try {
                                mediaCacheHandler.copyTo(stream, nnNewFile)
                                updateViewState(ChatMenuViewState.Closed)
                                updateFooterViewState(FooterViewState.Attachment)

                                attachmentSendStateContainer.updateViewState(
                                    AttachmentSendViewState.Preview(
                                        nnNewFile,
                                        mType,
                                        fileName?.toFileName(),
                                        null
                                    )
                                )
                            } catch (e: Exception) {
                                nnNewFile.delete()
                                LOG.e(
                                    TAG,
                                    "Failed to copy content to new file: ${nnNewFile.path}",
                                    e
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun confirmToggleBlockContactState() {

        val alertConfirmCallback: () -> Unit = {

            contactId?.let { contactId ->
                viewModelScope.launch(mainImmediate) {
                    contactRepository.getContactById(contactId).firstOrNull()?.let { contact ->
                        contactRepository.toggleContactBlocked(contact)
                    }
                }
            }
        }

        submitSideEffect(
            ChatSideEffect.AlertConfirmBlockContact {
                alertConfirmCallback().also {
                    viewModelScope.launch(mainImmediate) {
                        chatNavigator.popBackStack()
                    }
                }
            }
        )

    }

    suspend fun confirmDeleteContact() {
        val alertConfirmDeleteContact: () -> Unit = {
            contactId?.let { contactId ->
                viewModelScope.launch(mainImmediate) {
                    contactRepository.deleteContactById(contactId)
                }
            }
        }

        submitSideEffect(
            ChatSideEffect.AlertConfirmDeleteContact {
                alertConfirmDeleteContact().also {
                    viewModelScope.launch(mainImmediate) {
                        chatNavigator.popBackStack()
                    }
                }
            }
        )
    }

    fun sendCallInvite(audioOnly: Boolean) {
        callMenuHandler.updateViewState(
            MenuBottomViewState.Closed
        )

        val appContext: Context = app.applicationContext
        val serverUrlsSharedPreferences =
            appContext.getSharedPreferences("server_urls", Context.MODE_PRIVATE)

        val meetingServerUrl = serverUrlsSharedPreferences.getString(
            SphinxCallLink.CALL_SERVER_URL_KEY,
            SphinxCallLink.DEFAULT_CALL_SERVER_URL
        ) ?: SphinxCallLink.DEFAULT_CALL_SERVER_URL

        viewModelScope.launch(mainImmediate) {
            val chat = chatSharedFlow.firstOrNull()

            val messageText = if (chat?.isConversation() == true) {
                SphinxCallLink.newCallLinkMessage(meetingServerUrl, audioOnly, moshi)
            } else {
                SphinxCallLink.newCallLink(meetingServerUrl, audioOnly)
            }

            val isCall = (chat?.isConversation() == true)

            messageText?.let { newCallLink ->
                val messageBuilder = SendMessage.Builder()
                messageBuilder.setText(newCallLink)
                messageBuilder.setIsCall(isCall)
                sendMessage(messageBuilder)
            }
        }
    }

    fun copyCallLink(message: Message) {
        message.retrieveSphinxCallLink()?.let { callLink ->
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    ChatSideEffect.CopyCallLinkToClipboard(callLink.value)
                )
            }
        }
    }

    fun joinCall(message: Message, audioOnly: Boolean) {
        message.retrieveSphinxCallLink()?.let { sphinxCallLink ->
            joinCall(sphinxCallLink, audioOnly)
        }
    }

    private fun joinCall(link: SphinxCallLink, audioOnly: Boolean) {
        link.callServerUrl?.let { nnCallUrl ->

            viewModelScope.launch(mainImmediate) {

                val owner = getOwner()

                val userInfo = JitsiMeetUserInfo()
                userInfo.displayName = owner.alias?.value ?: ""

                owner.avatarUrl?.let { nnAvatarUrl ->
                    userInfo.avatar = nnAvatarUrl
                }

                val options = JitsiMeetConferenceOptions.Builder()
                    .setServerURL(nnCallUrl)
                    .setRoom(link.callRoom)
                    .setAudioMuted(false)
                    .setVideoMuted(false)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setAudioOnly(audioOnly)
                    .setUserInfo(userInfo)
                    .build()

                JitsiMeetActivity.launch(app, options)
            }
        }
    }

    internal val audioPlayerController: AudioPlayerController by lazy {
        AudioPlayerControllerImpl(
            app,
            viewModelScope,
            dispatchers,
            LOG,
        )
    }

    internal val audioRecorderController: AudioRecorderController<ARGS> by lazy {
        AudioRecorderController(
            viewModelScope = viewModelScope,
            mediaCacheHandler = mediaCacheHandler,
            updateDurationCallback = { duration ->
                updateFooterViewState(
                    FooterViewState.RecordingAudioAttachment(duration)
                )
            },
            dispatchers
        )
    }

    fun stopAndDeleteAudioRecording() {
        audioRecorderController.stopAndDeleteAudioRecording()
        updateFooterViewState(FooterViewState.Default)
    }

    fun goToChatDetailScreen() {
        audioPlayerController.pauseMediaIfPlaying()
        navigateToChatDetailScreen()
    }

    fun goToFullscreenVideo(
        messageId: MessageId,
        videoFilepath: String? = null
    ) {
        viewModelScope.launch(mainImmediate) {
            chatNavigator.toFullscreenVideo(
                messageId,
                videoFilepath
            )
        }
    }

    protected abstract fun navigateToChatDetailScreen()

    open fun handleContactTribeLinks(url: String?) {
        if (url != null) {

            viewModelScope.launch(mainImmediate) {

                url.toLightningNodePubKey()?.let { lightningNodePubKey ->

                    handleContactLink(lightningNodePubKey, null)

                } ?: url.toVirtualLightningNodeAddress()?.let { virtualNodeAddress ->

                    virtualNodeAddress.getPubKey()?.let { lightningNodePubKey ->
                        handleContactLink(
                            lightningNodePubKey,
                            virtualNodeAddress.getRouteHint()
                        )
                    }

                } ?: url.toTribeJoinLink()?.let { tribeJoinLink ->

                    handleTribeLink(tribeJoinLink)

                } ?: url.toFeedItemLink()?.let { feedItemLink ->

                    handleFeedItemLink(feedItemLink)

                }
            }

        }
    }

    private suspend fun goToFeedDetailView(feed: Feed) {
        when {
            feed.isPodcast -> {
                chatNavigator.toPodcastPlayer(
                    feed.chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    feed.id,
                    feed.feedUrl
                )
            }
            feed.isVideo -> {
                chatNavigator.toVideoWatchScreen(
                    feed.chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    feed.id,
                    feed.feedUrl
                )
            }
            feed.isNewsletter -> {
                chatNavigator.toNewsletterDetail(
                    feed.chat?.id ?: ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    feed.feedUrl
                )
            }
        }
    }

    private suspend fun handleFeedItemLink(link: FeedItemLink) {
        feedRepository.getFeedForLink(link).firstOrNull()?.let { feed ->
            goToFeedDetailView(feed)
        }
    }

    private suspend fun handleTribeLink(tribeJoinLink: TribeJoinLink) {
        chatRepository.getChatByUUID(ChatUUID(tribeJoinLink.tribeUUID)).firstOrNull()?.let { chat ->
            chatNavigator.toChat(chat, null, null)
        } ?: chatNavigator.toJoinTribeDetail(tribeJoinLink).also {
            audioPlayerController.pauseMediaIfPlaying()
        }
    }

    fun navigateToChatThread(uuid: MessageUUID) {
        viewModelScope.launch(mainImmediate) {
            chatNavigator.toChatThread(getChat().id, ThreadUUID(uuid.value))
        }
    }

    fun navigateToTribeFromThread() {
        viewModelScope.launch(mainImmediate) {
            chatNavigator.toChat(getChat(), null, null)
        }
    }

    private suspend fun handleContactLink(
        pubKey: LightningNodePubKey,
        routeHint: LightningRouteHint?
    ) {
        contactRepository.getContactByPubKey(pubKey).firstOrNull()?.let { contact ->

            chatRepository.getConversationByContactId(contact.id).firstOrNull().let { chat ->
                chatNavigator.toChat(chat, contact.id, null)
            }

        } ?: chatNavigator.toAddContactDetail(pubKey, routeHint).also {
            audioPlayerController.pauseMediaIfPlaying()
        }
    }

    open suspend fun processMemberRequest(
        contactId: ContactId,
        messageId: MessageId,
        type: MessageType,
    ) {
    }

    open suspend fun deleteTribe() {}

    open fun onSmallProfileImageClick(message: Message) {}

    open fun pinMessage(message: Message) {}

    open fun unPinMessage(message: Message? = null) {}

    override suspend fun onMotionSceneCompletion(value: Nothing) {
        // unused
    }

    fun saveFile(
        message: Message,
        drawable: Drawable?
    ) {
        viewModelScope.launch(mainImmediate) {
            if (message.isMediaAttachmentAvailable) {

                val originalMessageMessageMedia = message.messageMedia

                //Getting message media from purchase accept item if is paid.
                //LocalFile and mediaType should be returned from original message
                val mediaUrlAndMessageMedia = message.retrieveImageUrlAndMessageMedia()
                    ?: message.retrieveUrlAndMessageMedia()


                mediaUrlAndMessageMedia?.second?.let { messageMedia ->
                    originalMessageMessageMedia?.retrieveContentValues(message)
                        ?.let { mediaContentValues ->
                            originalMessageMessageMedia?.retrieveMediaStorageUri()
                                ?.let { mediaStorageUri ->
                                    app.contentResolver.insert(mediaStorageUri, mediaContentValues)
                                        ?.let { savedFileUri ->
                                            val inputStream: InputStream? = when {
                                                (drawable != null) -> {
                                                    drawable?.drawableToBitmap()?.toInputStream()
                                                }
                                                (originalMessageMessageMedia?.localFile != null) -> {
                                                    FileInputStream(originalMessageMessageMedia?.localFile)
                                                }
                                                else -> {
                                                    messageMedia.retrieveRemoteMediaInputStream(
                                                        mediaUrlAndMessageMedia.first,
                                                        memeServerTokenHandler,
                                                        memeInputStreamHandler
                                                    )
                                                }
                                            }

                                            try {
                                                inputStream?.use { nnInputStream ->
                                                    app.contentResolver.openOutputStream(
                                                        savedFileUri
                                                    ).use { savedFileOutputStream ->
                                                        if (savedFileOutputStream != null) {
                                                            nnInputStream.copyTo(
                                                                savedFileOutputStream,
                                                                1024
                                                            )

                                                            submitSideEffect(
                                                                ChatSideEffect.Notify(
                                                                    app.getString(
                                                                        R.string.saved_attachment_successfully
                                                                    )
                                                                )
                                                            )
                                                            return@launch
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                LOG.e(TAG, "Failed to store file: ", e)
                                            }

                                            submitSideEffect(
                                                ChatSideEffect.Notify(app.getString(R.string.failed_to_save_file))
                                            )
                                            try {
                                                app.contentResolver.delete(savedFileUri, null, null)
                                            } catch (fileE: Exception) {
                                                LOG.e(TAG, "Failed to delete file: ", fileE)
                                            }
                                        }
                                }
                        }
                }
            }
        }
    }

    fun showAttachmentImageFullscreen(message: Message) {
        message.retrieveImageUrlAndMessageMedia()?.let {
            updateAttachmentFullscreenViewState(
                AttachmentFullscreenViewState.ImageFullscreen(it.first, it.second)
            )
        }
    }

    fun navigateToPdfPage(pageDiff: Int) {
        val viewState = getAttachmentFullscreenViewStateFlow().value
        if (viewState is AttachmentFullscreenViewState.PdfFullScreen) {
            showAttachmentPdfFullscreen(null, viewState.currentPage + pageDiff)
        }
    }

    fun showAttachmentPdfFullscreen(
        message: Message?,
        page: Int
    ) {
        val fullscreenViewState = getAttachmentFullscreenViewStateFlow().value

        if (fullscreenViewState is AttachmentFullscreenViewState.PdfFullScreen) {
            updateAttachmentFullscreenViewState(
                AttachmentFullscreenViewState.PdfFullScreen(
                    fullscreenViewState.fileName,
                    fullscreenViewState.pdfRender.pageCount,
                    page,
                    fullscreenViewState.pdfRender
                )
            )
        } else {
            if (message?.messageMedia?.mediaType?.isPdf == true) {
                message.messageMedia?.localFile?.let { localFile ->

                    val pfd = ParcelFileDescriptor.open(localFile, MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)

                    updateAttachmentFullscreenViewState(
                        AttachmentFullscreenViewState.PdfFullScreen(
                            message.messageMedia?.fileName ?: FileName("File.txt"),
                            renderer.pageCount,
                            page,
                            renderer
                        )
                    )
                }
            }
        }
    }

    // TODO: Re-work to track messageID + job such that multiple paid messages can
    //  be fired at a time, but only once for that particular message until a response
    //  is had. Current implementation requires 1 Paid message confirmation to complete
    //  before allowing another one to be fired off.
    private var payAttachmentJob: Job? = null
    fun payAttachment(message: Message) {
        if (payAttachmentJob?.isActive == true) {
            return
        }

        val sideEffect = ChatSideEffect.AlertConfirmPayAttachment {
            payAttachmentJob = viewModelScope.launch(mainImmediate) {

                @Exhaustive
                when (val response = messageRepository.payAttachment(message)) {
                    is Response.Error -> {
                        submitSideEffect(ChatSideEffect.Notify(response.cause.message))
                    }
                    is Response.Success -> {
                        // give time for DB to push new data to render to screen
                        // to inhibit firing of another payAttachment
                        delay(100L)
                    }
                }
            }
        }

        viewModelScope.launch(mainImmediate) {
            submitSideEffect(sideEffect)
        }
    }

    private var payInvoiceJob: Job? = null
    fun payInvoice(message: Message) {
        if (payInvoiceJob?.isActive == true) {
            return
        }

        val sideEffect = ChatSideEffect.AlertConfirmPayInvoice {
            payInvoiceJob = viewModelScope.launch(mainImmediate) {

                @Exhaustive
                when (val response = messageRepository.payPaymentRequest(message)) {
                    is Response.Error -> {
                        submitSideEffect(ChatSideEffect.Notify(
                            String.format(
                                app.getString(R.string.error_payment_message),
                                response.exception?.message ?: response.cause.message
                            )
                        ))
                    }
                    is Response.Success -> {
                        delay(100L)
                    }
                }
            }
        }

        viewModelScope.launch(mainImmediate) {
            submitSideEffect(sideEffect)
        }
    }

    fun processMemberMention(s: CharSequence?) {
        val lastWord = s?.split(" ")?.last()?.toString() ?: ""

        if (lastWord.startsWith("@") && lastWord.length > 1) {
            val matchingMessages = messageHolderViewStateFlow.value.filter { messageHolder ->
                messageHolder.message?.senderAlias?.value?.let { member ->
                    (member.startsWith(lastWord.replace("@", ""), true))
                } ?: false
            }

            val matchingAliases = matchingMessages.map { it.message?.senderAlias?.value ?: "" }.distinct()

            messageMentionsViewStateContainer.updateViewState(
                MessageMentionsViewState.MessageMentions(matchingAliases)
            )

        } else {
            messageMentionsViewStateContainer.updateViewState(
                MessageMentionsViewState.MessageMentions(listOf())
            )
        }

    }

    fun sendAppLog(appLog: String) {
        actionsRepository.setAppLog(appLog)
    }

    override fun onCleared() {
        super.onCleared()
        (audioPlayerController as AudioPlayerControllerImpl).onCleared()
        audioRecorderController?.clear()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun MessageMedia.retrieveMediaStorageUri(): Uri? {
    return when {
        this.mediaType.isImage -> {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        this.mediaType.isVideo -> {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        this.mediaType.isAudio -> {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                null
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun MessageMedia.retrieveContentValues(message: Message): ContentValues? {
    val fileName = "${this.fileName?.value ?: message.id.value}"

    if (this.mediaType.isImage) {
        return ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, fileName)
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
    } else if (this.mediaType.isVideo) {
        return ContentValues().apply {
            put(MediaStore.Video.Media.TITLE, fileName)
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }
    } else if (this.mediaType.isPdf) {
        return ContentValues().apply {
            put(MediaStore.Downloads.TITLE, fileName)
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        }

    } else if (this.mediaType.isUnknown) {
        return ContentValues().apply {
            put(MediaStore.Downloads.TITLE, fileName)
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mediaType.value)
        }
    }
    return null
}


@Suppress("NOTHING_TO_INLINE")
suspend inline fun MessageMedia.retrieveRemoteMediaInputStream(
    url: String,
    memeServerTokenHandler: MemeServerTokenHandler,
    memeInputStreamHandler: MemeInputStreamHandler
): InputStream? {
    return localFile?.inputStream() ?: host?.let { mediaHost ->
        memeServerTokenHandler.retrieveAuthenticationToken(mediaHost)?.let { authenticationToken ->
            memeInputStreamHandler.retrieveMediaInputStream(
                url,
                authenticationToken,
                mediaKeyDecrypted
            )?.first
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Drawable.drawableToBitmap(): Bitmap? {
    return try {
        val bitDw = this as BitmapDrawable
        bitDw.bitmap
    } catch (e: Exception) {
        null
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Bitmap.toInputStream(): InputStream? {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 100, stream)
    val imageInByte: ByteArray = stream.toByteArray()
    return ByteArrayInputStream(imageInByte)
}


