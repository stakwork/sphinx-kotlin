package chat.sphinx.chat_tribe.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.chat_common.ui.ChatSideEffect
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.model.TribeFeedData
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_link_preview.LinkPreviewHandler
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.route.isRouteAvailable
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_podcast.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val ChatTribeFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class ChatTribeViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    memeServerTokenHandler: MemeServerTokenHandler,
    private val tribeChatNavigator: TribeChatNavigator,
    private val repositoryMedia: RepositoryMedia,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    networkQueryLightning: NetworkQueryLightning,
    mediaCacheHandler: MediaCacheHandler,
    savedStateHandle: SavedStateHandle,
    cameraViewModelCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    linkPreviewHandler: LinkPreviewHandler,
    memeInputStreamHandler: MemeInputStreamHandler,
    LOG: SphinxLogger,
): ChatViewModel<ChatTribeFragmentArgs>(
    app,
    dispatchers,
    memeServerTokenHandler,
    tribeChatNavigator,
    repositoryMedia,
    chatRepository,
    contactRepository,
    messageRepository,
    networkQueryLightning,
    mediaCacheHandler,
    savedStateHandle,
    cameraViewModelCoordinator,
    linkPreviewHandler,
    memeInputStreamHandler,
    LOG,
)
{
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
        } catch (e: Exception) {}
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

    override suspend fun getChatInfo(): Triple<ChatName?, PhotoUrl?, String>? {
        return null
    }

    override suspend fun shouldStreamSatsFor(podcastClip: PodcastClip, messageUUID: MessageUUID?) {
        getPodcast()?.let { podcast ->
            val metaData = ChatMetaData(
                itemId = podcastClip.itemID,
                itemLongId = ItemId(-1),
                satsPerMinute = getChat()?.metaData?.satsPerMinute ?: Sat(podcast.satsPerMinute),
                timeSeconds = podcastClip.ts,
                speed = 1.0
            )

            repositoryMedia.streamFeedPayments(
                chatId,
                metaData,
                podcastClip.feedID.value,
                podcastClip.itemID.value,
                podcast.getFeedDestinations(podcastClip.pubkey),
                false,
                messageUUID
            )
        }
    }

    override fun forceKeyExchange() { }

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

    override fun sendMessage(builder: SendMessage.Builder): SendMessage? {
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

                chatRepository.updateTribeInfo(chat)?.let { tribeData ->

                    _feedDataStateFlow.value = TribeFeedData.Result.FeedData(
                        tribeData.host,
                        tribeData.feedUrl,
                        tribeData.chatUUID,
                        tribeData.feedType,
                        chat.metaData,
                    )

                } ?: run {
                    _feedDataStateFlow.value = TribeFeedData.Result.NoFeed
                }

            } ?: run {
                _feedDataStateFlow.value = TribeFeedData.Result.NoFeed
            }
        }
    }

    override suspend fun processMemberRequest(
        contactId: ContactId,
        messageId: MessageId,
        type: MessageType,
    ) {
        viewModelScope.launch(mainImmediate) {
            val errorMessage = if (type.isMemberApprove()) {
                app.getString(R.string.failed_to_approve_member)
            } else {
                app.getString(R.string.failed_to_reject_member)
            }

            if (type.isMemberApprove() || type.isMemberReject()) {
                when(messageRepository.processMemberRequest(contactId, messageId, type)) {
                    is LoadResponse.Loading -> {}
                    is Response.Success -> {}

                    is Response.Error -> {
                        submitSideEffect(ChatSideEffect.Notify(errorMessage))
                    }
                }
            }
        }.join()
    }

    override suspend fun deleteTribe() {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                if (chat.type.isTribe()) {
                    when (chatRepository.exitAndDeleteTribe(chat)) {
                        is Response.Error -> {
                            submitSideEffect(ChatSideEffect.Notify(app.getString(R.string.failed_to_delete_tribe)))
                        }
                        is Response.Success -> {
                            chatNavigator.popBackStack()
                        }
                    }
                }
            }
        }.join()
    }

    override fun navigateToChatDetailScreen() {
        viewModelScope.launch(mainImmediate) {
            (chatNavigator as TribeChatNavigator).toTribeDetailScreen(chatId)
        }
    }
}
