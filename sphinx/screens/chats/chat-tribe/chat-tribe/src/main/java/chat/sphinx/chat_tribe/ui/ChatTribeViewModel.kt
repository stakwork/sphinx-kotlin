package chat.sphinx.chat_tribe.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.chat_common.ui.ChatSideEffect
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.menu.MoreMenuOptionsViewState
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.model.TribeFeedData
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.chat_tribe.ui.viewstate.*
import chat.sphinx.concept_link_preview.LinkPreviewHandler
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.route.isRouteAvailable
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_network_query_people.model.ChatLeaderboardDto
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard_android.RepositoryDashboardAndroid
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.message
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.FeedPlayerSpeed
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_podcast.Podcast
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val ChatTribeFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
class ChatTribeViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    memeServerTokenHandler: MemeServerTokenHandler,
    tribeChatNavigator: TribeChatNavigator,
    repositoryMedia: RepositoryMedia,
    feedRepository: FeedRepository,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    actionsRepository: ActionsRepository,
    repositoryDashboard: RepositoryDashboardAndroid<Any>,
    networkQueryLightning: NetworkQueryLightning,
    networkQueryPeople: NetworkQueryPeople,
    mediaCacheHandler: MediaCacheHandler,
    savedStateHandle: SavedStateHandle,
    cameraViewModelCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    linkPreviewHandler: LinkPreviewHandler,
    memeInputStreamHandler: MemeInputStreamHandler,
    moshi: Moshi,
    LOG: SphinxLogger,
): ChatViewModel<ChatTribeFragmentArgs>(
    app,
    dispatchers,
    memeServerTokenHandler,
    tribeChatNavigator,
    repositoryMedia,
    feedRepository,
    chatRepository,
    contactRepository,
    messageRepository,
    actionsRepository,
    repositoryDashboard,
    networkQueryLightning,
    networkQueryPeople,
    mediaCacheHandler,
    savedStateHandle,
    cameraViewModelCoordinator,
    linkPreviewHandler,
    memeInputStreamHandler,
    moshi,
    LOG,
) {
    override val args: ChatTribeFragmentArgs by savedStateHandle.navArgs()
    override val chatId: ChatId = args.chatId
    override val contactId: ContactId?
        get() = null

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private val podcastSharedFlow: SharedFlow<Podcast?> = flow {
        emitAll(chatRepository.getPodcastByChatId(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private val leaderboardListStateFlow: MutableStateFlow<List<ChatLeaderboardDto>?> by lazy {
        MutableStateFlow(null)
    }

    val tribeMemberProfileViewStateContainer: ViewStateContainer<TribeMemberProfileViewState> by lazy {
        ViewStateContainer(TribeMemberProfileViewState.Closed)
    }

    val tribeMemberDataViewStateContainer: ViewStateContainer<TribeMemberDataViewState> by lazy {
        ViewStateContainer(TribeMemberDataViewState.Idle)
    }

    val pinedMessagePopupViewState: ViewStateContainer<PinedMessagePopupViewState> by lazy {
        ViewStateContainer(PinedMessagePopupViewState.Idle)
    }

    val pinedMessageBottomViewState: ViewStateContainer<PinMessageBottomViewState> by lazy {
        ViewStateContainer(PinMessageBottomViewState.Closed)
    }

    val pinedMessageDataViewState: ViewStateContainer<PinedMessageDataViewState> by lazy {
        ViewStateContainer(PinedMessageDataViewState.Idle)
    }

    private suspend fun getPodcast(): Podcast? {
        podcastSharedFlow.replayCache.firstOrNull()?.let { podcast ->
            return podcast
        }

        podcastSharedFlow.firstOrNull()?.let { podcast ->
            return podcast
        }

        var podcast: Podcast? = null

        try {
            podcastSharedFlow.collect {
                if (it != null) {
                    podcast = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {
        }
        delay(25L)
        return podcast
    }

    override val headerInitialHolderSharedFlow: SharedFlow<InitialHolderViewState> = flow {
        chatSharedFlow.collect { chat ->
            chat?.photoUrl?.let {
                emit(
                    InitialHolderViewState.Url(it)
                )
            } ?: chat?.name?.let {
                emit(
                    InitialHolderViewState.Initials(
                        it.value.getInitials(),
                        chat.getColorKey()
                    )
                )
            } ?: emit(
                InitialHolderViewState.None
            )
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    override val threadSharedFlow: SharedFlow<List<Message>>? =
        if (args.argThreadUUID.isNullOrEmpty()){
            null
        } else
            flow {
                messageRepository.getThreadUUIDMessagesByUUID(chatId, ThreadUUID(args.argThreadUUID!!)).collect {
                    emit(it)
                }
            }.distinctUntilChanged().shareIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(2_000),
                replay = 1,
            )

    internal val moreOptionsMenuStateFlow: MutableStateFlow<MoreMenuOptionsViewState> by lazy {
        MutableStateFlow(MoreMenuOptionsViewState.OwnTribe)
    }

    override suspend fun getChatInfo(): Triple<ChatName?, PhotoUrl?, String>? {
        return null
    }

    override suspend fun shouldStreamSatsFor(podcastClip: PodcastClip, messageUUID: MessageUUID?) {
        getPodcast()?.let { podcast ->
            feedRepository.streamFeedPayments(
                chatId,
                podcastClip.feedID.value,
                podcastClip.itemID.value,
                podcastClip.ts.toLong(),
                getChat()?.metaData?.satsPerMinute ?: Sat(podcast.satsPerMinute),
                FeedPlayerSpeed(1.0),
                podcast.getFeedDestinations(podcastClip.pubkey),
                messageUUID
            )
        }
    }

    override fun forceKeyExchange() {}

    override suspend fun getInitialHolderViewStateForReceivedMessage(
        message: Message,
        owner: Contact
    ): InitialHolderViewState {
        return message.senderPic?.let { url ->
            InitialHolderViewState.Url(url)
        } ?: message.senderAlias?.let { alias ->
            InitialHolderViewState.Initials(alias.value.getInitials(), message.getColorKey())
        } ?: InitialHolderViewState.None
    }

    override val checkRoute: Flow<LoadResponse<Boolean, ResponseError>> = flow {
        networkQueryLightning.checkRoute(chatId).collect { response ->
            @Exhaustive
            when (response) {
                is LoadResponse.Loading -> {
                    emit(response)
                }
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emit(Response.Success(response.value.isRouteAvailable))
                }
            }
        }
    }

    override fun readMessages() {
        viewModelScope.launch(mainImmediate) {
            messageRepository.readMessages(chatId)
        }
    }

    override suspend fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        args.argThreadUUID?.let {
            builder.setThreadUUID(ThreadUUID(it))
        }
        builder.setChatId(chatId)
        return super.sendMessage(builder)
    }

    private val _feedDataStateFlow: MutableStateFlow<TribeFeedData> by lazy {
        MutableStateFlow(TribeFeedData.Loading)
    }

    val feedDataStateFlow: StateFlow<TribeFeedData>
        get() = _feedDataStateFlow.asStateFlow()

    init {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                moreOptionsMenuStateFlow.value =
                    if (chat.isTribeOwnedByAccount(getOwner().nodePubKey)) {
                        MoreMenuOptionsViewState.OwnTribe
                    } else {
                        MoreMenuOptionsViewState.NotOwnTribe
                    }

                chatRepository.updateTribeInfo(chat)?.let { tribeData ->

                    if (!args.argThreadUUID.isNullOrEmpty()) {
                        _feedDataStateFlow.value = TribeFeedData.Result.NoFeed
                    }
                    else {
                        _feedDataStateFlow.value = TribeFeedData.Result.FeedData(
                            tribeData.host,
                            tribeData.feedUrl,
                            tribeData.chatUUID,
                            tribeData.feedType,
                            tribeData.appUrl,
                            tribeData.badges
                        )
                    }

                    updatePinnedMessageState(tribeData.pin, chat.id)

                } ?: run {
                    _feedDataStateFlow.value = TribeFeedData.Result.NoFeed
                }

            } ?: run {
                _feedDataStateFlow.value = TribeFeedData.Result.NoFeed
            }
        }

        getAllLeaderboards()
    }

    override suspend fun processMemberRequest(
        chatId: ChatId,
        messageUuid: MessageUUID,
        type: MessageType.GroupAction,
        senderAlias: SenderAlias?,
    ) {
        viewModelScope.launch(mainImmediate) {
            val errorMessage = if (type.isMemberApprove()) {
                app.getString(R.string.failed_to_approve_member)
            } else {
                app.getString(R.string.failed_to_reject_member)
            }

            if (type.isMemberApprove() || type.isMemberReject()) {
                messageRepository.processMemberRequest(chatId, messageUuid, null, type, senderAlias)
//                when () {
//                    is LoadResponse.Loading -> {}
//                    is Response.Success -> {}
//
//                    is Response.Error -> {
//                        submitSideEffect(ChatSideEffect.Notify(errorMessage))
//                    }
//                }
            }
        }.join()
    }

    override suspend fun deleteTribe() {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                if (chat.type.isTribe()) {
                    chatRepository.exitAndDeleteTribe(chat)
                    chatNavigator.popBackStack()
                }
            }
        }.join()
    }

    override fun onSmallProfileImageClick(message: Message) {
        showMemberPopup(message)
    }

    override fun pinMessage(message: Message) {

        viewModelScope.launch(mainImmediate) {

            val response = chatRepository.pinMessage(chatId, message)

            if (response is Response.Success) {
                showPinMessagePopup(app.getString(R.string.message_pinned))
            } else {
                submitSideEffect(ChatSideEffect.Notify(app.getString(R.string.pin_message_error)))
            }
        }
    }

    override fun unPinMessage(message: Message?) {
        viewModelScope.launch(mainImmediate) {

            (message ?: (pinedMessageDataViewState.value as? PinedMessageDataViewState.Data)?.message)?.let { nnMessage ->

                pinedMessageBottomViewState.updateViewState(
                    PinMessageBottomViewState.Closed
                )

                val response = chatRepository.unPinMessage(chatId, nnMessage)

                if (response is Response.Success) {
                    showPinMessagePopup(app.getString(R.string.message_unpinned))
                } else {
                    submitSideEffect(ChatSideEffect.Notify(app.getString(R.string.pin_message_error)))
                }
            }
        }
    }

    private fun showPinMessagePopup(
        text: String
    ) {
        viewModelScope.launch(mainImmediate) {
            pinedMessagePopupViewState.updateViewState(
                PinedMessagePopupViewState.Visible(text)
            )

            delay(1000L)

            pinedMessagePopupViewState.updateViewState(
                PinedMessagePopupViewState.Idle
            )
        }
    }

    fun showPinBottomView() {
        pinedMessageBottomViewState.updateViewState(
            PinMessageBottomViewState.Open
        )
    }

    private fun showMemberPopup(message: Message) {
        message.person?.let { _ ->
            tribeMemberDataViewStateContainer.updateViewState(
                TribeMemberDataViewState.LoadingTribeMemberProfile
            )

            tribeMemberProfileViewStateContainer.updateViewState(
                TribeMemberProfileViewState.Open
            )

            loadPersonData(message)

        } ?: message.uuid?.let { messageUUID ->
            message.senderAlias?.let { senderAlias ->

                tribeMemberDataViewStateContainer.updateViewState(
                    TribeMemberDataViewState.TribeMemberPopup(
                        messageUUID,
                        senderAlias,
                        message.getColorKey(),
                        message.senderPic
                    )
                )
            }
        }
    }

    private fun getAllLeaderboards(){
        viewModelScope.launch(mainImmediate) {
           val tribeUUID = getChat().uuid
            networkQueryPeople.getLeaderboard(tribeUUID).collect { loadResponse ->
                when(loadResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {}
                    is Response.Success -> {
                        leaderboardListStateFlow.value = loadResponse.value
                    }
                }
            }
        }
    }

    private fun loadPersonData(message: Message) {
        viewModelScope.launch(mainImmediate) {
            message.person?.let { person ->
                networkQueryPeople.getTribeMemberProfile(person).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}

                        is Response.Error -> {
                            submitSideEffect(ChatSideEffect.Notify(loadResponse.message))
                            tribeMemberProfileViewStateContainer.updateViewState(
                                TribeMemberProfileViewState.Closed
                            )
                        }
                        is Response.Success -> {
                            val leaderboard = leaderboardListStateFlow.value?.find { it.alias == loadResponse.value.owner_alias  }

                            networkQueryPeople.getBadgesByPerson(person).collect { badgesResponse ->
                                when (badgesResponse) {
                                    is LoadResponse.Loading -> {}
                                    is Response.Error -> {
                                        tribeMemberDataViewStateContainer.updateViewState(
                                            TribeMemberDataViewState.TribeMemberProfile(
                                                message.uuid,
                                                loadResponse.value,
                                                leaderboard,
                                                null
                                            )
                                        )
                                    }
                                    is Response.Success -> {
                                        tribeMemberDataViewStateContainer.updateViewState(
                                            TribeMemberDataViewState.TribeMemberProfile(
                                                message.uuid,
                                                loadResponse.value,
                                                leaderboard,
                                                badgesResponse.value
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun goToPaymentSend() {
        viewModelScope.launch(mainImmediate) {
            val messageUUID = (tribeMemberDataViewStateContainer.value as? TribeMemberDataViewState.TribeMemberPopup)?.messageUUID ?:
                (tribeMemberDataViewStateContainer.value as? TribeMemberDataViewState.TribeMemberProfile)?.messageUUID

            messageUUID?.let { nnMessageUUID ->
                chatNavigator.toPaymentSendDetail(
                    nnMessageUUID,
                    chatId
                )
            }

            if (tribeMemberDataViewStateContainer.value !is TribeMemberDataViewState.Idle) {
                tribeMemberDataViewStateContainer.updateViewState(
                    TribeMemberDataViewState.Idle
                )
            }

            if (tribeMemberProfileViewStateContainer.value is TribeMemberProfileViewState.Open) {
                tribeMemberProfileViewStateContainer.updateViewState(
                    TribeMemberProfileViewState.Closed
                )
            }
        }
    }

    fun goToKnownBadges() {
        viewModelScope.launch(mainImmediate) {
            (chatNavigator as TribeChatNavigator).toKnownBadges(
                badgeIds = (feedDataStateFlow.value as? TribeFeedData.Result.FeedData)?.badges ?: arrayOf()
            )
        }
    }

    override fun navigateToChatDetailScreen() {
        viewModelScope.launch(mainImmediate) {
            (chatNavigator as TribeChatNavigator).toTribeDetailScreen(chatId)
        }
    }

    fun navigateToTribeShareScreen() {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()
            val shareTribeURL = "sphinx.chat://?action=tribe&uuid=${chat.uuid.value}&host=${chat.host?.value}"
            (chatNavigator as TribeChatNavigator).toShareTribeScreen(shareTribeURL, app.getString(R.string.qr_code_title))
        }

        moreOptionsMenuHandler.updateViewState(MenuBottomViewState.Closed)
    }

    override fun navigateToNotificationLevel() {
        moreOptionsMenuHandler.updateViewState(MenuBottomViewState.Closed)

        viewModelScope.launch(mainImmediate) {
            (chatNavigator as TribeChatNavigator).toNotificationsLevel(chatId)
        }
    }

    fun navigateToThreads() {
        viewModelScope.launch(mainImmediate) {
            (chatNavigator as TribeChatNavigator).toThreads(chatId)
        }
    }

    override fun getThreadUUID(): ThreadUUID? {
        return args.argThreadUUID?.toThreadUUID()
    }

    override fun isThreadChat(): Boolean {
        return getThreadUUID() != null
    }

    override fun reloadPinnedMessage() {
        viewModelScope.launch(mainImmediate) {
            getChat()?.let { nnChat ->

                (pinedMessageDataViewState.value as? PinedMessageDataViewState.Data)?.let { viewState ->
                    if (viewState.message.uuid == nnChat.pinedMessage) {
                        return@launch
                    }
                }

                updatePinnedMessageState(nnChat.pinedMessage, nnChat.id)
            }
        }
    }

    private suspend fun updatePinnedMessageState(
        messageUUID: MessageUUID?,
        chatId: ChatId
    ) {
        if (isThreadChat()) {
            return
        }

        messageUUID?.let { uuid ->
            if (uuid.value.isNotEmpty()) {
                messageRepository.getMessageByUUID(uuid).firstOrNull()?.let { message ->
                    pinedMessageDataViewState.updateViewState(
                        getPinnedMessageData(message)
                    )
                    return
                } ?: run {
                    messageRepository.fetchPinnedMessageByUUID(uuid, chatId)
                }
            }
        }
        pinedMessageDataViewState.updateViewState(
            PinedMessageDataViewState.Idle
        )
    }

    private suspend fun getPinnedMessageData(message: Message): PinedMessageDataViewState {
        var pinnedMessageData: PinedMessageDataViewState = PinedMessageDataViewState.Idle

        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()
            val isOwner = getChat().isTribeOwnedByAccount(getOwner().nodePubKey)
            val messageContent = message.retrieveTextToShow()
            val senderAlias = if (isOwner) owner.alias?.value else message.senderAlias?.value
            val senderPic = if (isOwner) owner.photoUrl else message.senderPic

            messageContent?.let {
                pinnedMessageData = PinedMessageDataViewState.Data(
                    message = message,
                    messageContent = messageContent,
                    isOwnTribe = isOwner,
                    senderAlias = senderAlias ?: "Unknown",
                    senderPic = senderPic,
                    senderColorKey = message.getColorKey()
                )
            }
        }.join()

        return pinnedMessageData
    }

}
