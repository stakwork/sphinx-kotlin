package chat.sphinx.podcast_player.ui

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player.objects.toPodcast
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal inline val PodcastPlayerFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class PodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PodcastPlayerNavigator,
    protected val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    private val mediaPlayerServiceController: MediaPlayerServiceController
) : BaseViewModel<PodcastPlayerViewState>(
    dispatchers,
    PodcastPlayerViewState.Idle
), MediaPlayerServiceController.MediaServiceListener {

    private val args: PodcastPlayerFragmentArgs by savedStateHandle.navArgs()

    val podcast: Podcast = args.argPodcast.toPodcast()

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != args.chatId) {
                return
            }
        }

        @Exhaustive
        when (serviceState) {
            is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                podcast.playingEpisodeUpdate(serviceState.episodeId, serviceState.currentTime, serviceState.episodeDuration)
                viewStateContainer.updateViewState(PodcastPlayerViewState.MediaStateUpdate(podcast, serviceState))
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                podcast.pauseEpisodeUpdate()
                viewStateContainer.updateViewState(PodcastPlayerViewState.MediaStateUpdate(podcast, serviceState))
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                podcast.endEpisodeUpdate(serviceState.episodeId, ::retrieveEpisodeDuration)
                viewStateContainer.updateViewState(PodcastPlayerViewState.MediaStateUpdate(podcast, serviceState))
            }
            is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {
                setPaymentsDestinations()
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
            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                chat.metaData?.let { metaData ->
                    podcast?.setMetaData(metaData)
                }
            }
            viewStateContainer.updateViewState(PodcastPlayerViewState.PodcastLoaded(podcast))

            mediaPlayerServiceController.submitAction(
                UserAction.AdjustSatsPerMinute(
                    args.chatId,
                    podcast.getMetaData()
                )
            )
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
                    podcast.id,
                    episode.id,
                    podcast.episodeDuration ?: 0,
                    episode.enclosureUrl,
                    Sat(podcast.satsPerMinute),
                    podcast.speed,
                    startTime,
                )
            )

            withContext(io) {
                podcast.didStartPlayingEpisode(episode, startTime, ::retrieveEpisodeDuration)
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

    private fun setPaymentsDestinations() {
        podcast?.value?.destinations?.let { destinations ->
            viewModelScope.launch(mainImmediate) {
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
}

fun Uri.getMediaDuration(): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        if (Build.VERSION.SDK_INT >= 14) {
            retriever.setDataSource(this.toString(), HashMap<String, String>())
        } else {
            retriever.setDataSource(this.toString())
        }
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        duration?.toLongOrNull() ?: 0
    } catch (exception: Exception) {
        0
    }
}
