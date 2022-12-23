package chat.sphinx.common_player.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.common_player.viewstate.BoostAnimationViewState
import chat.sphinx.common_player.viewstate.PlayerViewState
import chat.sphinx.common_player.viewstate.RecommendationsPodcastPlayerViewState
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.wrapper_action_track.action_wrappers.VideoRecordConsumed
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal inline val CommonPlayerScreenFragmentArgs.podcastId: FeedId
    get() = FeedId(argPodcastId)

internal inline val CommonPlayerScreenFragmentArgs.episodeId: FeedId
    get() = FeedId(argEpisodeId)

@HiltViewModel
class CommonPlayerScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: CommonPlayerNavigator,
    private val contactRepository: ContactRepository,
    private val feedRepository: FeedRepository,
    private val actionsRepository: ActionsRepository,
    private val moshi: Moshi,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        CommonPlayerScreenSideEffect,
        RecommendationsPodcastPlayerViewState,
        >(dispatchers, RecommendationsPodcastPlayerViewState.Idle), MediaPlayerServiceController.MediaServiceListener
{

    private val args: CommonPlayerScreenFragmentArgs by savedStateHandle.navArgs()

    val boostAnimationViewStateContainer: ViewStateContainer<BoostAnimationViewState> by lazy {
        ViewStateContainer(BoostAnimationViewState.Idle)
    }

    val playerViewStateContainer: ViewStateContainer<PlayerViewState> by lazy {
        ViewStateContainer(PlayerViewState.Idle)
    }

    private var videoRecordConsumed: VideoRecordConsumed? = null

    private val podcastSharedFlow: SharedFlow<Podcast?> = flow {
        emitAll(feedRepository.getPodcastById(args.podcastId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getOwner(): Contact {
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

    init {
        mediaPlayerServiceController.addListener(this)

        loadRecommendationsPodcast()

        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()

            boostAnimationViewStateContainer.updateViewState(
                BoostAnimationViewState.BoosAnimationInfo(
                    owner.photoUrl,
                    owner.tipAmount
                )
            )
        }
    }

    private fun loadRecommendationsPodcast() {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getPodcastById(
                FeedId(args.argPodcastId)
            ).firstOrNull()?.let { podcast ->
                podcastLoaded(podcast)
            } ?: run {
                submitSideEffect(
                    CommonPlayerScreenSideEffect.Notify.ErrorLoadingRecommendations
                )

                navigator.closeDetailScreen()
            }
        }
    }

    private fun podcastLoaded(podcast: Podcast) {
        viewModelScope.launch(mainImmediate) {

            viewStateContainer.updateViewState(
                RecommendationsPodcastPlayerViewState.PodcastViewState.PodcastLoaded(podcast)
            )

            val currentEpisode = podcast.getCurrentEpisode()

            if (!currentEpisode.playing) {
                podcast.setCurrentEpisodeWith(args.episodeId.value)
                val newEpisode = podcast.getCurrentEpisode()

                playerViewStateContainer.updateViewState(
                    if (newEpisode.isYouTubeVideo) {
                        PlayerViewState.YouTubeVideoSelected(podcast.getCurrentEpisode())
                    } else {
                        PlayerViewState.PodcastEpisodeSelected
                    }
                )

                if (newEpisode.isMusicClip) {
                    viewStateContainer.updateViewState(
                        RecommendationsPodcastPlayerViewState.PodcastViewState.LoadingEpisode(podcast, newEpisode)
                    )

                    delay(300L)
                    playEpisode(newEpisode, newEpisode.clipStartTime ?: podcast.currentTime)
                }
            } else {
                playerViewStateContainer.updateViewState(
                    if (currentEpisode.isYouTubeVideo) {
                        PlayerViewState.YouTubeVideoSelected(currentEpisode)
                    } else {
                        PlayerViewState.PodcastEpisodeSelected
                    }
                )
            }
        }
    }

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.podcastId != args.podcastId.value) {
                return
            }
        }

        viewModelScope.launch(mainImmediate) {
            getPodcast()?.let { podcast ->
                @Exhaustive
                when (serviceState) {
                    is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                        podcast.playingEpisodeUpdate(
                            serviceState.episodeId,
                            serviceState.currentTime,
                            serviceState.episodeDuration.toLong(),
                            serviceState.speed
                        )
                        viewStateContainer.updateViewState(
                            RecommendationsPodcastPlayerViewState.PodcastViewState.MediaStateUpdate(
                                podcast,
                                serviceState
                            )
                        )
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                        podcast.pauseEpisodeUpdate()
                        viewStateContainer.updateViewState(
                            RecommendationsPodcastPlayerViewState.PodcastViewState.MediaStateUpdate(
                                podcast,
                                serviceState
                            )
                        )
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                        podcast.endEpisodeUpdate(
                            serviceState.episodeId,
                            ::retrieveEpisodeDuration
                        )
                        viewStateContainer.updateViewState(
                            RecommendationsPodcastPlayerViewState.PodcastViewState.MediaStateUpdate(
                                podcast,
                                serviceState
                            )
                        )
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Failed -> {
                        submitSideEffect(
                            CommonPlayerScreenSideEffect.Notify.ErrorPlayingClip
                        )
                    }
                    is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {}

                    is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                        viewStateContainer.updateViewState(RecommendationsPodcastPlayerViewState.ServiceLoading)
                    }
                    is MediaPlayerServiceState.ServiceInactive -> {
                        podcast.pauseEpisodeUpdate()
                        viewStateContainer.updateViewState(RecommendationsPodcastPlayerViewState.ServiceInactive)
                    }
                }
            }
        }
    }

    fun playEpisodeFromList(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            getPodcast()?.let { podcast ->
                viewStateContainer.updateViewState(
                    RecommendationsPodcastPlayerViewState.PodcastViewState.LoadingEpisode(podcast, episode)
                )

                if (episode.isMusicClip) {
                    playerViewStateContainer.updateViewState(
                        PlayerViewState.PodcastEpisodeSelected
                    )

                    delay(50L)

                    playEpisode(
                        episode,
                        episode.clipStartTime ?: 0
                    )
                } else if (episode.isYouTubeVideo) {
                    playerViewStateContainer.updateViewState(
                        PlayerViewState.YouTubeVideoSelected(episode)
                    )

                    getPodcast()?.let { podcast ->
                        podcast.getCurrentEpisode()?.let { episode ->
                            pauseEpisode(episode)
                        }
                        podcast.setCurrentEpisodeWith(episode.id.value)
                    }
                }
            }
        }
    }

    fun playEpisode(episode: PodcastEpisode, startTime: Int) {
        viewModelScope.launch(mainImmediate) {
            getPodcast()?.let { podcast ->
                viewModelScope.launch(mainImmediate) {
                    mediaPlayerServiceController.submitAction(
                        UserAction.ServiceAction.Play(
                            ChatId(ChatId.NULL_CHAT_ID.toLong()),
                            podcast.id.value,
                            episode.id.value,
                            episode.episodeUrl,
                            Sat(podcast.satsPerMinute),
                            podcast.speed,
                            startTime,
                        )
                    )

                    withContext(io) {
                        podcast.didStartPlayingEpisode(
                            episode,
                            startTime,
                            ::retrieveEpisodeDuration
                        )
                    }

                    viewStateContainer.updateViewState(
                        RecommendationsPodcastPlayerViewState.PodcastViewState.EpisodePlayed(
                            podcast
                        )
                    )
                }
            }
        }
    }

    fun pauseEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            getPodcast()?.let { podcast ->
                podcast.didPausePlayingEpisode(episode)

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Pause(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        episode.id.value
                    )
                )
            }
        }
    }

    fun seekTo(time: Int) {
        viewModelScope.launch(mainImmediate) {
            getPodcast()?.let { podcast ->
                podcast.didSeekTo(time)

                val metaData = podcast.getMetaData()

                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Seek(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        metaData
                    )
                )
            }
        }
    }

    fun adjustSpeed(speed: Double) {
        viewModelScope.launch(mainImmediate) {
            getPodcast()?.let { podcast ->
                podcast.speed = speed

                mediaPlayerServiceController.submitAction(
                    UserAction.AdjustSpeed(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        podcast.getMetaData()
                    )
                )
            }
        }
    }

    fun retrieveEpisodeDuration(episodeUrl: String, localFile: File?): Long {
        localFile?.let {
            return Uri.fromFile(it).getMediaDuration(true)
        } ?: run {
            return Uri.parse(episodeUrl).getMediaDuration(false)
        }
    }

    suspend fun playingVideoDidPause() {
        (playerViewStateContainer.value as? PlayerViewState.YouTubeVideoSelected)?.let {
            getPodcast()?.getCurrentEpisode()?.playing = false
        }
    }

    suspend fun playingVideoUpdate() {
        (playerViewStateContainer.value as? PlayerViewState.YouTubeVideoSelected)?.let {
            getPodcast()?.getCurrentEpisode()?.playing = true
        }
    }

    fun trackPodcastConsumed() {
        viewModelScope.launch(mainImmediate) {
            mediaPlayerServiceController.submitAction(
                UserAction.TrackPodcastConsumed(
                    ChatId(ChatId.NULL_CHAT_ID.toLong())
                )
            )
        }
    }

    fun createVideoRecordConsumed(feedItemId: FeedId){
        if (videoRecordConsumed?.feedItemId == feedItemId){
            return
        }
        videoRecordConsumed = VideoRecordConsumed(feedItemId)
    }

    fun trackVideoConsumed(){
        videoRecordConsumed?.let { record ->
            if (record.history.isNotEmpty()) {
                actionsRepository.trackMediaContentConsumed(
                    record.feedItemId,
                    record.history
                )
            }
        }
    }
    fun setNewHistoryItem(videoPosition: Long){
        videoRecordConsumed?.setNewHistoryItem(videoPosition)
    }

    fun startTimer() {
        videoRecordConsumed?.startTimer()
    }

    fun createHistoryItem() {
        videoRecordConsumed?.createHistoryItem()
    }

    fun stopTimer(){
        videoRecordConsumed?.stopTimer()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
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