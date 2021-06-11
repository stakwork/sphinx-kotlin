package chat.sphinx.podcast_player.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal inline val PodcastPlayerFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

internal inline val PodcastPlayerFragmentArgs.podcast: Podcast
    get() = argPodcast

@HiltViewModel
internal class PodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PodcastPlayerNavigator,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    private val mediaPlayerServiceController: MediaPlayerServiceController
) : BaseViewModel<PodcastPlayerViewState>(
    dispatchers,
    PodcastPlayerViewState.Idle
), MediaPlayerServiceController.MediaServiceListener {

    //    private val chatSharedFlow: SharedFlow<Chat?> = flow {
    //        emitAll(chatRepository.getChatById(args.chatId))
    //    }.distinctUntilChanged().shareIn(
    //        viewModelScope,
    //        SharingStarted.WhileSubscribed(2_000),
    //        replay = 1,
    //    )

    private val args: PodcastPlayerFragmentArgs by savedStateHandle.navArgs()

    var podcast: Podcast? = null

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
            viewStateContainer.updateViewState(PodcastPlayerViewState.MediaStateUpdate(podcast))
        }
    }

    init {
        mediaPlayerServiceController.addListener(this)

        args.podcast?.let { argPodcast ->
            podcast = argPodcast

            viewStateContainer.updateViewState(PodcastPlayerViewState.PodcastLoaded(argPodcast))
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
    }

    fun playEpisode(episode: PodcastEpisode, startTime: Int) {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                chat?.let { chat ->
                    podcast?.let { podcast ->
                        viewStateContainer.updateViewState(PodcastPlayerViewState.LoadingEpisode(episode))

                        withContext(io) {
                            podcast.didStartPlayingEpisode(episode, startTime)
                        }

                        viewStateContainer.updateViewState(
                            PodcastPlayerViewState.EpisodePlayed(
                                podcast
                            )
                        )

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Play(
                                chat.id,
                                episode.id,
                                episode.enclosureUrl,
                                startTime,
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
                                episode.id,
                                Sat(0)
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

    fun adjustSpeed(speed: Double) {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                podcast?.let { podcast ->
                    podcast.speed = speed

                    val metaData = podcast.getMetaData()

                    mediaPlayerServiceController.submitAction(
                        UserAction.AdjustSpeed(
                            chat.id,
                            metaData
                        )
                    )
                }
            }
        }
    }
}
