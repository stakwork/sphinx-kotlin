package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.chat_common.ui.ChatSideEffect
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.menu.ChatMenuViewState
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_chat.model.toPodcast
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.route.isRouteAvailable
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.*
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.podcast_player.objects.toParcelablePodcast
import chat.sphinx.podcast_player.ui.getMediaDuration
import chat.sphinx.resources.getRandomColor
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message.isMemberApprove
import chat.sphinx.wrapper_message.isMemberReject
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val ChatTribeFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class ChatTribeViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    memeServerTokenHandler: MemeServerTokenHandler,
    chatNavigator: TribeChatNavigator,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    networkQueryLightning: NetworkQueryLightning,
    mediaCacheHandler: MediaCacheHandler,
    savedStateHandle: SavedStateHandle,
    cameraViewModelCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    LOG: SphinxLogger,
    private val mediaPlayerServiceController: MediaPlayerServiceController
): ChatViewModel<ChatTribeFragmentArgs>(
    app,
    dispatchers,
    memeServerTokenHandler,
    chatNavigator,
    chatRepository,
    contactRepository,
    messageRepository,
    networkQueryLightning,
    mediaCacheHandler,
    savedStateHandle,
    cameraViewModelCoordinator,
    LOG,
), MediaPlayerServiceController.MediaServiceListener
{
    override val args: ChatTribeFragmentArgs by savedStateHandle.navArgs()
    override val chatId: ChatId = args.chatId
    override val contactId: ContactId?
        get() = null

    val podcastViewStateContainer: ViewStateContainer<PodcastViewState> by lazy {
        ViewStateContainer(PodcastViewState.Idle)
    }

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    var podcast: Podcast? = null

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
                        headerInitialsTextViewColor
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

    override suspend fun getChatNameIfNull(): ChatName? {
        return null
    }

    override suspend fun getInitialHolderViewStateForReceivedMessage(
        message: Message
    ): InitialHolderViewState {
        return message.senderPic?.let { url ->
            InitialHolderViewState.Url(url)
        } ?: message.senderAlias?.let { alias ->
            InitialHolderViewState.Initials(alias.value.getInitials(), app.getRandomColor())
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

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != chatId) {
                return
            }
        }

        podcast?.let { podcast ->
            @Exhaustive
            when (serviceState) {
                is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                    podcast.playingEpisodeUpdate(serviceState.episodeId, serviceState.currentTime, serviceState.episodeDuration.toLong())
                    podcastViewStateContainer.updateViewState(PodcastViewState.MediaStateUpdate(podcast))
                }
                is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                    podcast.pauseEpisodeUpdate()
                    podcastViewStateContainer.updateViewState(PodcastViewState.MediaStateUpdate(podcast))
                }
                is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                    podcast.endEpisodeUpdate(serviceState.episodeId, ::retrieveEpisodeDuration)
                    podcastViewStateContainer.updateViewState(PodcastViewState.MediaStateUpdate(podcast))
                }
                is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {
                    setPaymentsDestinations()
                }
                is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                    podcastViewStateContainer.updateViewState(PodcastViewState.ServiceLoading)
                }
                is MediaPlayerServiceState.ServiceInactive -> {
                    podcast.pauseEpisodeUpdate()
                    podcastViewStateContainer.updateViewState(PodcastViewState.ServiceInactive)
                }
            }
        }
    }

    init {
        mediaPlayerServiceController.addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
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

    suspend fun loadTribeAndPodcastData(): Podcast? {
        chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->

            chatRepository.updateTribeInfo(chat)?.let { podcastDto ->
                podcast = podcastDto.toPodcast()

                chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                    chat.metaData?.let { metaData ->
                        podcast?.setMetaData(metaData)
                    }
                }

                mediaPlayerServiceController.submitAction(
                    UserAction.AdjustSatsPerMinute(
                        args.chatId,
                        podcast!!.getMetaData()
                    )
                )
            }
        }

        return podcast
    }

    fun getPodcastContributionsString(): Flow<String> = flow {
        podcast?.id?.let { podcastId ->
            val owner: Contact = contactRepository.accountOwner.value.let { contact ->
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

            chatRepository.getChatById(chatId).firstOrNull()?.let { chat ->
                messageRepository.getPaymentsTotalFor(podcastId).collect { paymentsTotal ->
                    paymentsTotal?.let {
                        val isMyTribe = chat.isTribeOwnedByAccount(owner.nodePubKey)
                        val label = app.getString(if (isMyTribe) R.string.chat_tribe_earned else R.string.chat_tribe_contributed)

                        emit(
                            label + " ${it.asFormattedString()} ${it.unit}"
                        )
                    }
                }
            }
        }
    }

    fun goToPodcastPlayerScreen() {
        podcast?.let { podcast ->
            viewModelScope.launch(mainImmediate) {
                (chatNavigator as TribeChatNavigator).toPodcastPlayerScreen(chatId, podcast.toParcelablePodcast())
            }
        }
    }

    fun playPausePodcast() {
        podcast?.let { podcast ->
            podcast.getCurrentEpisode().let { currentEpisode ->
                if (currentEpisode.playing) {
                    pauseEpisode(currentEpisode)
                } else {
                    playEpisode(currentEpisode, podcast.currentTime)
                }
            }
        }
    }

    private fun playEpisode(episode: PodcastEpisode, startTime: Int) {
        viewModelScope.launch(mainImmediate) {
            podcast?.let { podcast ->
                withContext(io) {
                    podcast.didStartPlayingEpisode(episode, startTime, ::retrieveEpisodeDuration)
                }

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Play(
                        chatId,
                        podcast.id,
                        episode.id,
                        episode.enclosureUrl,
                        Sat(podcast.satsPerMinute),
                        podcast.speed,
                        startTime,
                    )
                )
            }
        }
    }

    private fun pauseEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            podcast?.let { podcast ->
                podcast.didPausePlayingEpisode(episode)

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Pause(chatId, episode.id)
                )
            }
        }
    }

    fun seekTo(time: Int) {
        viewModelScope.launch(mainImmediate) {
            podcast?.let { podcast ->
                podcast.didSeekTo(podcast.currentTime + time)

                val metaData = podcast.getMetaData()

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Seek(chatId, metaData)
                )
            }
        }
    }

    private fun setPaymentsDestinations() {
        viewModelScope.launch(mainImmediate) {
            podcast?.value?.destinations?.let { destinations ->
                mediaPlayerServiceController.submitAction(
                    UserAction.SetPaymentsDestinations(
                        args.chatId,
                        destinations
                    )
                )
            }
        }
    }

    fun retrieveEpisodeDuration(episodeUrl: String): Long {
        val uri = Uri.parse(episodeUrl)
        return uri.getMediaDuration()
    }
    
    override fun goToChatDetailScreen() {
        viewModelScope.launch(mainImmediate) {
            (chatNavigator as TribeChatNavigator).toTribeDetailScreen(chatId, podcast?.toParcelablePodcast())
        }
    }
}
