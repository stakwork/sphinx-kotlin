package chat.sphinx.dashboard.ui

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.dashboard.ui.viewstates.*
import chat.sphinx.dashboard.ui.viewstates.PlayingPodcastViewState.NoPodcast.clickBoost
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_podcast.FeedRecommendation
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class DashboardPodcastViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    private val dashboardNavigator: DashboardNavigator,
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
                        vs.podcast.willStartPlayingEpisode(
                            episode,
                            vs.podcast.timeMilliSeconds,
                            ::retrieveEpisodeDuration,
                        )

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Play(
                                podcast.chatId,
                                episode.episodeUrl,
                                vs.podcast.getUpdatedContentFeedStatus(),
                                vs.podcast.getUpdatedContentEpisodeStatus()
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
                    vs.podcast.didSeekTo(vs.podcast.timeMilliSeconds + 30_000L)

                    mediaPlayerServiceController.submitAction(
                        UserAction.ServiceAction.Seek(
                            podcast.chatId,
                            vs.podcast.getUpdatedContentEpisodeStatus()
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

                val contentFeedStatus = vs.podcast.getUpdatedContentFeedStatus()

                feedRepository.updateContentFeedStatus(
                    vs.podcast.id,
                    contentFeedStatus.feedUrl,
                    contentFeedStatus.subscriptionStatus,
                    podcast.chatId,
                    contentFeedStatus.itemId,
                    contentFeedStatus.satsPerMinute,
                    contentFeedStatus.playerSpeed
                )

                val contentEpisodeStatus = vs.podcast.getUpdatedContentEpisodeStatus()

                contentEpisodeStatus?.itemId?.let {episodeId ->
                    feedRepository.updateContentEpisodeStatus(
                        vs.podcast.id,
                        episodeId,
                        contentEpisodeStatus.duration,
                        contentEpisodeStatus.currentTime
                    )
                }

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
                    vs.podcast.getCurrentEpisode().id
                )
            } else {
                dashboardNavigator.toPodcastPlayerScreen(
                    vs.podcast.chatId,
                    vs.podcast.id,
                    vs.podcast.feedUrl
                )
            }
        }
    }

    private fun retrieveEpisodeDuration(
        episode: PodcastEpisode
    ): Long {
        val duration = episode.localFile?.let {
            Uri.fromFile(it).getMediaDuration(true)
        } ?: Uri.parse(episode.episodeUrl).getMediaDuration(false)

        viewModelScope.launch(io) {
            feedRepository.updateContentEpisodeStatus(
                feedId = episode.podcastId,
                itemId = episode.id,
                FeedItemDuration(duration / 1000),
                FeedItemDuration(episode.currentTimeSeconds)
            )
        }

        return duration
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


