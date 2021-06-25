package chat.sphinx.podcast_player.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal inline val PodcastPlayerFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class PodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PodcastPlayerNavigator,
    savedStateHandle: SavedStateHandle,
    private val mediaPlayerServiceController: MediaPlayerServiceController
) : BaseViewModel<PodcastPlayerViewState>(
    dispatchers,
    PodcastPlayerViewState.Idle
), MediaPlayerServiceController.MediaServiceListener {

    private val args: PodcastPlayerFragmentArgs by savedStateHandle.navArgs()

    val podcast: Podcast = args.argPodcast

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != args.chatId) {
                return
            }
        }

        @Exhaustive
        when (serviceState) {
            is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                podcast.playingEpisodeUpdate(serviceState.episodeId, serviceState.currentTime)
                viewStateContainer.updateViewState(PodcastPlayerViewState.MediaStateUpdate(podcast, serviceState))
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                podcast.pauseEpisodeUpdate()
                viewStateContainer.updateViewState(PodcastPlayerViewState.MediaStateUpdate(podcast, serviceState))
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                podcast.endEpisodeUpdate(serviceState.episodeId)
                viewStateContainer.updateViewState(PodcastPlayerViewState.MediaStateUpdate(podcast, serviceState))
            }
            is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                viewStateContainer.updateViewState(PodcastPlayerViewState.ServiceLoading)
            }
            is MediaPlayerServiceState.ServiceInactive -> {
                podcast.pauseEpisodeUpdate()
                viewStateContainer.updateViewState(PodcastPlayerViewState.ServiceInactive)
            }
        }
    }

    init {
        mediaPlayerServiceController.addListener(this)
        podcastLoaded()
    }

    private fun podcastLoaded() {
        viewModelScope.launch(mainImmediate) {
            delay(100L)
            viewStateContainer.updateViewState(PodcastPlayerViewState.PodcastLoaded(podcast))
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
    }

    fun playEpisodeFromList(episode: PodcastEpisode, startTime: Int) {
        viewModelScope.launch(mainImmediate) {
            viewStateContainer.updateViewState(PodcastPlayerViewState.LoadingEpisode(episode))

            delay(50L)

            playEpisode(episode, startTime)
        }
    }

    fun playEpisode(episode: PodcastEpisode, startTime: Int) {
        viewModelScope.launch(mainImmediate) {
            mediaPlayerServiceController.submitAction(
                UserAction.ServiceAction.Play(
                    args.chatId,
                    episode.id,
                    episode.enclosureUrl,
                    Sat(0),
                    podcast.speed,
                    startTime,
                )
            )

            withContext(io) {
                podcast.didStartPlayingEpisode(episode, startTime)
            }

            viewStateContainer.updateViewState(PodcastPlayerViewState.EpisodePlayed(podcast))
        }
    }

    fun pauseEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            podcast.didPausePlayingEpisode(episode)

            mediaPlayerServiceController.submitAction(
                UserAction.ServiceAction.Pause(
                    args.chatId,
                    episode.id
                )
            )
        }
    }

    fun seekTo(time: Int) {
        viewModelScope.launch(mainImmediate) {
            podcast.didSeekTo(time)

            val metaData = podcast.getMetaData()

            mediaPlayerServiceController.submitAction(
                UserAction.ServiceAction.Seek(
                    args.chatId,
                    metaData
                )
            )
        }
    }

    fun adjustSpeed(speed: Double) {
        viewModelScope.launch(mainImmediate) {
            podcast.speed = speed

            mediaPlayerServiceController.submitAction(
                UserAction.AdjustSpeed(
                    args.chatId,
                    podcast.getMetaData()
                )
            )
        }
    }
}
