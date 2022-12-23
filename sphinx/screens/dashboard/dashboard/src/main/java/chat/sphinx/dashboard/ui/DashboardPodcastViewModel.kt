package chat.sphinx.dashboard.ui

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.*
import chat.sphinx.dashboard.ui.viewstates.DashboardPodcastViewState
import chat.sphinx.dashboard.ui.viewstates.OnClickCallback
import chat.sphinx.dashboard.ui.viewstates.PlayingPodcastViewState
import chat.sphinx.dashboard.ui.viewstates.PlayingPodcastViewState.NoPodcast.clickBoost
import chat.sphinx.dashboard.ui.viewstates.adjustState
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_podcast.FeedRecommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class DashboardPodcastViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    private val dashboardNavigator: DashboardNavigator,
    private val repositoryMedia: RepositoryMedia,
    private val feedRepository: FeedRepository,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
) : BaseViewModel<DashboardPodcastViewState>(dispatchers, DashboardPodcastViewState.Idle),
    MediaPlayerServiceController.MediaServiceListener
{
    val playingPodcastViewStateContainer: ViewStateContainer<PlayingPodcastViewState> by lazy {
        ViewStateContainer(PlayingPodcastViewState.NoPodcast)
    }

    init {
        mediaPlayerServiceController.addListener(this@DashboardPodcastViewModel)

        viewModelScope.launch(mainImmediate) {
            feedRepository.recommendationsPodcast.collect {
                (playingPodcastViewStateContainer.value as? PlayingPodcastViewState.PodcastVS)?.let { viewState ->
                    if (viewState.podcast.id?.value == FeedRecommendation.RECOMMENDATION_PODCAST_ID) {
                        playingPodcastViewStateContainer.updateViewState(PlayingPodcastViewState.NoPodcast)
                    }
                }
            }
        }
    }

    private var currentServiceState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceInactive
    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        currentServiceState = serviceState

        val vs = playingPodcastViewStateContainer.value

        if (vs !is PlayingPodcastViewState.PodcastVS) {
            loadPodcastFrom(serviceState)
            return
        }

        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.podcastId != vs.podcast.id.value) {
                loadPodcastFrom(serviceState)
                return
            }
        }

        when (serviceState) {
            is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                vs.podcast.playingEpisodeUpdate(
                    serviceState.episodeId,
                    serviceState.currentTime,
                    serviceState.episodeDuration.toLong(),
                    serviceState.speed
                )

                vs.adjustState(
                    showLoading = false,
                    showPlayButton = false,
                    title = vs.podcast.getCurrentEpisode().title.value,
                    imageUrl = vs.podcast.imageToShow?.value,
                    playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                )?.let {
                    playingPodcastViewStateContainer.updateViewState(it)
                }
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                vs.podcast.pauseEpisodeUpdate()

                vs.adjustState(
                    showLoading = false,
                    showPlayButton = true,
                    title = vs.podcast.getCurrentEpisode().title.value,
                    imageUrl = vs.podcast.imageToShow?.value,
                    playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                )?.let {
                    playingPodcastViewStateContainer.updateViewState(it)
                }
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                vs.podcast.endEpisodeUpdate(
                    serviceState.episodeId,
                    ::retrieveEpisodeDuration
                )

                vs.adjustState(
                    showLoading = false,
                    showPlayButton = true,
                    title = vs.podcast.getCurrentEpisode().title.value,
                    imageUrl = vs.podcast.imageToShow?.value,
                    playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                )?.let {
                    playingPodcastViewStateContainer.updateViewState(it)
                }
            }
            else -> {}
        }
    }

    override fun onCleared() {
        mediaPlayerServiceController.removeListener(this)
    }

    private fun loadPodcastFrom(
        serviceState: MediaPlayerServiceState
    ) {
        if (serviceState !is MediaPlayerServiceState.ServiceActive.MediaState) {
            return
        }

        val feedId = serviceState.podcastId.toFeedId() ?: run {
            return
        }

        viewModelScope.launch(mainImmediate) {

            val podcast = feedRepository.getPodcastById(feedId).firstOrNull() ?: run {
                return@launch
            }

            val clickPlayPause = OnClickCallback {
                val vs = playingPodcastViewStateContainer.value

                if (vs !is PlayingPodcastViewState.PodcastVS) {
                    return@OnClickCallback
                }

                val episode = vs.podcast.getCurrentEpisode()

                viewModelScope.launch {
                    if (episode.playing) {
                        vs.podcast.didPausePlayingEpisode(episode)

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Pause(
                                podcast.chatId,
                                episode.id.value
                            )
                        )
                    } else {
                        withContext(io) {
                            vs.podcast.didStartPlayingEpisode(
                                episode,
                                vs.podcast.currentTime,
                                ::retrieveEpisodeDuration,
                            )
                        }

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Play(
                                podcast.chatId,
                                vs.podcast.id.value,
                                episode.id.value,
                                episode.episodeUrl,
                                Sat(vs.podcast.satsPerMinute),
                                vs.podcast.speed,
                                vs.podcast.currentTime,
                            )
                        )
                    }
                }
            }

            val clickFastForward = OnClickCallback {
                val vs = playingPodcastViewStateContainer.value

                if (vs !is PlayingPodcastViewState.PodcastVS) {
                    return@OnClickCallback
                }

                viewModelScope.launch(mainImmediate) {
                    vs.podcast.didSeekTo(vs.podcast.currentTime + 30_000)

                    mediaPlayerServiceController.submitAction(
                        UserAction.ServiceAction.Seek(
                            podcast.chatId,
                            vs.podcast.getMetaData()
                        )
                    )

                    if (!vs.podcast.isPlaying) {
                        vs.adjustState(
                            showPlayButton = true,
                            playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                        )?.let {
                            playingPodcastViewStateContainer.updateViewState(it)
                        }
                    }
                }
            }

            val clickTitle = OnClickCallback {
                val vs = playingPodcastViewStateContainer.value

                if (vs !is PlayingPodcastViewState.PodcastVS) {
                    return@OnClickCallback
                }

                repositoryMedia.updateChatMetaData(
                    podcast.chatId,
                    vs.podcast.id,
                    vs.podcast.getMetaData(),
                    false
                )

                requestPodcastPlayer(vs)
            }

            val isPlaying =
                (currentServiceState is MediaPlayerServiceState.ServiceActive.MediaState.Playing)

            val currentSS = currentServiceState

            val playingProgress =
                if (currentSS is MediaPlayerServiceState.ServiceActive.MediaState.Playing) {
                    podcast.getPlayingProgress(currentSS.episodeDuration)
                } else {
                    withContext(io) { podcast.getPlayingProgress(::retrieveEpisodeDuration) }
                }

            PlayingPodcastViewState.PodcastVS.Ready(
                showLoading = false,
                showPlayButton = !isPlaying,
                title = podcast.getCurrentEpisode().title.value,
                subtitle = podcast.author?.value ?: podcast.getCurrentEpisode().showTitle?.value ?: podcast.title.value,
                imageUrl = podcast.imageToShow?.value,
                playingProgress = playingProgress,
                clickPlayPause = clickPlayPause,
                clickBoost = clickBoost,
                clickFastForward = clickFastForward,
                clickTitle = clickTitle,
                podcast
            ).let { initialViewState ->
                playingPodcastViewStateContainer.updateViewState(initialViewState)
            }
        }
    }

    fun forcePodcastStop() {
        val isPlaying =
            (currentServiceState is MediaPlayerServiceState.ServiceActive.MediaState.Playing)

        if (isPlaying) {
            playingPodcastViewStateContainer.value.clickPlayPause?.invoke()
        }

        playingPodcastViewStateContainer.updateViewState(PlayingPodcastViewState.NoPodcast)
    }

    fun trackPodcastConsumed() {
        viewModelScope.launch(mainImmediate) {
            (currentServiceState as? MediaPlayerServiceState.ServiceActive.MediaState)?.let {
                it.podcastId.toFeedId()?.let { feedId ->
                    feedRepository.getPodcastById(feedId).firstOrNull()?.let { podcast ->
                        mediaPlayerServiceController.submitAction(
                            UserAction.TrackPodcastConsumed(
                                podcast.chatId
                            )
                        )
                    }
                }
            }
        }
    }

    private fun requestPodcastPlayer(
        vs: PlayingPodcastViewState.PodcastVS
    ) {
        viewModelScope.launch(mainImmediate) {
            if (vs.podcast.id.value == FeedRecommendation.RECOMMENDATION_PODCAST_ID) {
                dashboardNavigator.toCommonPlayerScreen(
                    vs.podcast.id,
                    vs.podcast.getCurrentEpisode().id,
                    vs.podcast.episodeDuration ?: 0
                )
            } else {
                dashboardNavigator.toPodcastPlayerScreen(
                    vs.podcast.chatId,
                    vs.podcast.id,
                    vs.podcast.feedUrl,
                    vs.podcast.episodeDuration ?: 0
                )
            }
        }
    }

    private fun retrieveEpisodeDuration(episodeUrl: String, localFile: File?): Long {
        localFile?.let {
            return Uri.fromFile(it).getMediaDuration(true)
        } ?: run {
            return Uri.parse(episodeUrl).getMediaDuration(false)
        }
    }
}

fun Uri.getMediaDuration(
    isLocalFile: Boolean
): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        if (Build.VERSION.SDK_INT >= 14 && !isLocalFile) {
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


