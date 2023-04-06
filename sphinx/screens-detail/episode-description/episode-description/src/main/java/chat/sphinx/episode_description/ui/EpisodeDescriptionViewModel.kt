package chat.sphinx.episode_description.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.create_description.R
import chat.sphinx.episode_description.model.EpisodeDescription
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.hhmmElseDate
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedItemDetail
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import chat.sphinx.wrapper_podcast.toHrAndMin
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EpisodeDescriptionViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    val navigator: EpisodeDescriptionNavigator,
): SideEffectViewModel<
        Context,
        EpisodeDescriptionSideEffect,
        EpisodeDescriptionViewState,
        >(dispatchers, EpisodeDescriptionViewState.Idle),
    MediaPlayerServiceController.MediaServiceListener {

    private val args: EpisodeDescriptionFragmentArgs by savedStateHandle.navArgs()
    var podcast: Podcast? = null

    private val _feedItemEpisodeStateFlow: MutableStateFlow<Pair<FeedItem, PodcastEpisode?>?> by lazy {
        MutableStateFlow(null)
    }

    private val feedItemEpisodeStateFlow: StateFlow<Pair<FeedItem, PodcastEpisode?>?>
        get() = _feedItemEpisodeStateFlow.asStateFlow()

    init {
        mediaPlayerServiceController.addListener(this)

        viewModelScope.launch(mainImmediate) {
            args.argFeedId.toFeedId()?.let { nnFeedId ->
                feedRepository.getFeedItemById(nnFeedId).collect { feedItem ->
                    feedItem?.let { nnFeedItem ->
                        feedRepository.getPodcastById(nnFeedItem.feedId).collect {
                            podcast = it
                            val feed = feedRepository.getFeedById(nnFeedItem.feedId).firstOrNull()
                            val podcastEpisode = podcast?.getEpisodeWithId(feedItem.id.value)

                            _feedItemEpisodeStateFlow.value = Pair(nnFeedItem, podcastEpisode)

                            val episodeDescription = EpisodeDescription(
                                feedId = nnFeedItem.id,
                                header = nnFeedItem.titleToShow,
                                description = nnFeedItem.descriptionToShow,
                                image = nnFeedItem.imageUrlToShow?.value ?: "",
                                episodeTypeImage = getFeedItemDrawableType(feed?.feedType),
                                episodeTypeText = getFeedItemStringType(feed?.feedType),
                                episodeDate = nnFeedItem.datePublished?.hhmmElseDate() ?: "",
                                episodeDuration = podcastEpisode?.getUpdatedContentEpisodeStatus()?.duration?.value?.toInt()
                                    ?.toHrAndMin() ?: "",
                                downloaded = podcastEpisode?.downloaded,
                                isDownloadInProgress = isFeedItemDownloadInProgress(),
                                isEpisodeSoundPlaying = isEpisodeSoundPlaying(),
                                link = nnFeedItem.link?.value ?: "",
                                played = getPlayedMark(nnFeedItem.id),
                                feedName = feed?.titleToShow
                            )
                            handleUpdateViewState(episodeDescription)
                        }
                    }
                }
            }
        }
    }

    private fun handleUpdateViewState(episodeDescription: EpisodeDescription) {
        if (currentViewState is EpisodeDescriptionViewState.FeedItemDetails) {
            updateViewState(EpisodeDescriptionViewState.FeedItemDetails(episodeDescription))
        } else
        {
            updateViewState(EpisodeDescriptionViewState.FeedItemDescription(episodeDescription))
        }
    }

    private fun retrieveEpisodeDescription(): EpisodeDescription? {
        return when (val state = currentViewState) {
            is EpisodeDescriptionViewState.FeedItemDetails -> state.feedItemDescription
            is EpisodeDescriptionViewState.FeedItemDescription -> state.feedItemDescription
            else -> null
        }
    }

    private fun isFeedItemDownloadInProgress(): Boolean {
        feedItemEpisodeStateFlow.value?.first?.id.let { feedItemId ->
            return repositoryMedia.inProgressDownloadIds().contains(feedItemId)
        }
    }

    private fun isEpisodeSoundPlaying(): Boolean {
        feedItemEpisodeStateFlow.value?.second?.playing.let { playing ->
            return playing == true && mediaPlayerServiceController.getPlayingContent()?.third == true
        }
    }

    private suspend fun getPlayedMark(feedItemId: FeedId): Boolean {
        return feedRepository.getPlayedMark(feedItemId).firstOrNull() ?: false
    }

    fun playEpisodeFromDescription() {
        viewModelScope.launch(mainImmediate) {
            feedItemEpisodeStateFlow.value?.second?.let { podcastEpisode ->
                if (mediaPlayerServiceController.getPlayingContent()?.second == podcastEpisode.id.value) {
                    pauseEpisode(podcastEpisode)
                } else {
                    delay(50L)
                    playEpisode(podcastEpisode)
                }
            }
        }
    }
    private fun playEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            podcast?.let { nnPodcast ->
                nnPodcast.willStartPlayingEpisode(
                episode,
                episode.currentTimeMilliseconds ?: 0,
                ::retrieveEpisodeDuration
            )
                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Play(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        episode.episodeUrl,
                        nnPodcast.getUpdatedContentFeedStatus(),
                        nnPodcast.getUpdatedContentEpisodeStatus()
                    )
                )
            }
        }
    }

    private fun pauseEpisode(episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            podcast?.didPausePlayingEpisode(episode)

            mediaPlayerServiceController.submitAction(
                UserAction.ServiceAction.Pause(
                    ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    episode.id.value
                )
            )
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

    fun share(context: Context, label: String) {
        feedItemEpisodeStateFlow.value?.second?.episodeUrl?.let { url ->
            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }

            context.startActivity(
                Intent.createChooser(
                    sharingIntent,
                    label
                )
            )
        }
    }

    fun downloadMedia() {
        feedItemEpisodeStateFlow.value?.second?.let { podcastEpisode ->
            repositoryMedia.downloadMediaIfApplicable(
                podcastEpisode
            ) { downloadedFile ->
                podcastEpisode.localFile = downloadedFile
            }
            _feedItemEpisodeStateFlow.value = _feedItemEpisodeStateFlow.value?.copy(second = podcastEpisode)

            val downloaded = feedItemEpisodeStateFlow.value?.second?.downloaded
            retrieveEpisodeDescription()?.copy(
                downloaded = downloaded, isDownloadInProgress = isFeedItemDownloadInProgress())?.let { episodeDescription ->
                handleUpdateViewState(episodeDescription)
            }
        }
    }
    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.episodeId != args.argFeedId) {
                return
            }
        }
        viewModelScope.launch(mainImmediate) {
            podcast?.let { nnPodcast ->
                    when (serviceState) {
                        is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                            nnPodcast.playingEpisodeUpdate(
                                serviceState.episodeId,
                                serviceState.currentTime,
                                serviceState.episodeDuration.toLong(),
                                serviceState.speed
                            )
                            retrieveEpisodeDescription()?.copy(isEpisodeSoundPlaying = true)?.let { episodeDescription ->
                                handleUpdateViewState(episodeDescription)
                            }
                        }
                        is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                            nnPodcast.pauseEpisodeUpdate()

                            retrieveEpisodeDescription()?.copy(isEpisodeSoundPlaying = false)?.let { episodeDescription ->
                                handleUpdateViewState(episodeDescription)
                            }
                        }
                        is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                            nnPodcast.endEpisodeUpdate(
                                serviceState.episodeId,
                                ::retrieveEpisodeDuration
                            )
                            retrieveEpisodeDescription()?.copy(isEpisodeSoundPlaying = false)?.let { episodeDescription ->
                                handleUpdateViewState(episodeDescription)
                            }

                            feedRepository.updatePlayedMark(nnPodcast.id, true)
                        }
                        else -> {}
                    }
                }
            }
        }
}
    private fun getFeedItemDrawableType(feedType: FeedType?): Int {
        return when (feedType) {
            is FeedType.Podcast -> R.drawable.ic_podcast_type
            is FeedType.Video -> R.drawable.ic_youtube_type
            else -> {
                R.drawable.ic_podcast_placeholder
            }
        }
    }

    private fun getFeedItemStringType(feedType: FeedType?): String {
        return when (feedType) {
            is FeedType.Podcast -> "Podcast"
            is FeedType.Video -> "Youtube"
            else -> {
                ""
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
