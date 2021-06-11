package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.ChatSideEffect
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderFooterViewState
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.route.isRouteAvailable
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.SendMessage
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import chat.sphinx.podcast_player.objects.toPodcast
import chat.sphinx.resources.getRandomColor
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
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
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    networkQueryLightning: NetworkQueryLightning,
    savedStateHandle: SavedStateHandle,
    private val mediaPlayerServiceController: MediaPlayerServiceController
): ChatViewModel<ChatTribeFragmentArgs>(
    app,
    dispatchers,
    chatRepository,
    contactRepository,
    messageRepository,
    networkQueryLightning,
    savedStateHandle,
), MediaPlayerServiceController.MediaServiceListener
{
    override val args: ChatTribeFragmentArgs by savedStateHandle.navArgs()

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(args.chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    var podcast: Podcast? = null

    @Inject
    lateinit var chatNavigator: TribeChatNavigator

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
        networkQueryLightning.checkRoute(args.chatId).collect { response ->
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
            messageRepository.readMessages(args.chatId)
        }
    }

    override fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        builder.setChatId(args.chatId)
        return super.sendMessage(builder)
    }

//    private val _mediaPlayerServiceStateFlow: MutableStateFlow<MediaPlayerServiceState> by lazy {
//        MutableStateFlow(MediaPlayerServiceState.ServiceInactive)
//    }
//    val mediaPlayerServiceStateFlow: StateFlow<MediaPlayerServiceState>
//        get() = _mediaPlayerServiceStateFlow

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
//        _mediaPlayerServiceStateFlow.value = serviceState

        podcast?.let { podcast ->
            when (serviceState) {
                is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                    podcast.playingEpisodeUpdate(serviceState.episodeId, serviceState.currentTime.toInt())
                }
                is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                    podcast.pauseEpisodeUpdate(serviceState.episodeId)
                }
                is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                    podcast.endEpisodeUpdate(serviceState.episodeId)
                }
                is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {}
                is MediaPlayerServiceState.ServiceInactive -> {}
            }
            viewStateContainer.updateViewState(ChatHeaderFooterViewState.MediaStateUpdate(podcast))
        }
    }

    init {
        mediaPlayerServiceController.addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
    }

    private var updateTribeInfoJob: Job? = null
    fun loadTribeAndPodcastData(): Flow<Podcast> = flow {
        chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
            chatRepository.updateTribeInfo(chat).collect { podcastDto ->
                podcast = podcastDto.toPodcast()

                delay(10L)
                updateTribeInfoJob?.join()

                chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                    val pricePerMessage = chat.pricePerMessage?.value ?: 0
                    val escrowAmount = chat.escrowAmount?.value ?: 0

                    submitSideEffect(
                        ChatSideEffect.Notify(
                            "Price per message: $pricePerMessage\n Amount to Stake: $escrowAmount"
                        )
                    )

                    chat.metaData?.let { metaData ->
                        podcast?.setMetaData(metaData)
                    }
                }
            }

            Log.d(TAG, "Price per message ${chat.pricePerMessage.toString()}")
        }

        podcast?.let { podcast ->
            emit(podcast)
        }
    }

    fun goToPodcastPlayerScreen(podcast: Podcast) {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                chatNavigator.toPodcastPlayerScreen(chat.id, podcast)
            }
        }
    }

    fun playEpisode(episode: PodcastEpisode, startTime: Int) {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                chat?.let { chat ->
                    podcast?.let { podcast ->
                        withContext(io) {
                            podcast.didStartPlayingEpisode(episode, startTime)
                        }

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Play(
                                chat.id,
                                episode.id,
                                episode.enclosureUrl,
                                startTime.toLong(),
                            )
                        )
                    }
                }
            }
        }
    }

    fun pauseEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                chat?.let { chat ->
                    podcast?.let { podcast ->
                        podcast.didStopPlayingEpisode(episode)

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Pause(
                                chat.id,
                                episode.id
                            )
                        )
                    }
                }
            }
        }
    }

    fun seekTo(time: Int) {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                chat?.let { chat ->
                    podcast?.let { podcast ->
                        podcast.didSeekTo(time)

                        val metaData = podcast.getMetaData()

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Seek(
                                chat.id,
                                metaData
                            )
                        )
                    }
                }
            }
        }
    }
}
