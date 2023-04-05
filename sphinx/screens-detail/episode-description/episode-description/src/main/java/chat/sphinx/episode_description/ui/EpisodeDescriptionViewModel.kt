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
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
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

    init {
        mediaPlayerServiceController.addListener(this)
        viewModelScope.launch(mainImmediate) {
            args.argFeedId.toFeedId()?.let { nnFeedId ->
                feedRepository.getFeedItemById(nnFeedId).collect { feedItem ->
                    feedItem?.let { mmFeedItem ->
                        feedRepository.getPodcastById(feedItem.feedId).collect {
                            podcast = it
                            val feed = feedRepository.getFeedById(feedItem.feedId).firstOrNull()
                            val podcastEpisode = podcast?.getEpisodeWithId(feedItem.id.value)
                            updateViewState(
                                EpisodeDescriptionViewState.FeedItemDescription(
                                    mmFeedItem,
                                    feed,
                                    podcastEpisode,
                                    isFeedItemDownloadInProgress(),
                                    isEpisodeSoundPlaying()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isFeedItemDownloadInProgress(): Boolean {
        (currentViewState as? EpisodeDescriptionViewState.FeedItemDescription)?.feedItem?.id.let { feedItemId  ->
            return repositoryMedia.inProgressDownloadIds().contains(feedItemId)
        }
    }

    private fun isEpisodeSoundPlaying(): Boolean {
        (currentViewState as? EpisodeDescriptionViewState.FeedItemDescription)?.podcastEpisode?.playing.let { playing ->
            return playing == true && mediaPlayerServiceController.getPlayingContent()?.third == true
        }
    }

    fun playEpisodeFromDescription() {
        viewModelScope.launch(mainImmediate) {
            val currentViewState = (currentViewState as? EpisodeDescriptionViewState.FeedItemDescription)
                currentViewState?.podcastEpisode?.let { podcastEpisode ->
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
        (currentViewState as? EpisodeDescriptionViewState.FeedItemDescription)?.podcastEpisode?.episodeUrl?.let { url ->

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
        val currentViewState = (currentViewState as? EpisodeDescriptionViewState.FeedItemDescription)
        currentViewState?.podcastEpisode?.let { podcastEpisode ->
            repositoryMedia.downloadMediaIfApplicable(
                podcastEpisode
            ) { downloadedFile ->
                podcastEpisode.localFile = downloadedFile
            }
            updateViewState(
                EpisodeDescriptionViewState.FeedItemDescription(
                    currentViewState.feedItem,
                    currentViewState.feed,
                    podcastEpisode,
                    isFeedItemDownloadInProgress(),
                    currentViewState.isEpisodeSoundPlaying
                )
            )
        }
    }
    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.episodeId != args.argFeedId) {
                return
            }
        }
        viewModelScope.launch(mainImmediate) {
            val currentViewState = (currentViewState as? EpisodeDescriptionViewState.FeedItemDescription)
            currentViewState?.podcastEpisode?.let { podcastEpisode ->
                podcast?.let { nnPodcast ->
                    when (serviceState) {
                        is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                            nnPodcast.playingEpisodeUpdate(
                                serviceState.episodeId,
                                serviceState.currentTime,
                                serviceState.episodeDuration.toLong(),
                                serviceState.speed
                            )
                            updateViewState(EpisodeDescriptionViewState.FeedItemDescription(
                                currentViewState.feedItem,
                                currentViewState.feed,
                                podcastEpisode,
                                isFeedItemDownloadInProgress(),
                                true
                            ))
                        }
                        is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                            nnPodcast.pauseEpisodeUpdate()
                            updateViewState(EpisodeDescriptionViewState.FeedItemDescription(
                                currentViewState.feedItem,
                                currentViewState.feed,
                                podcastEpisode,
                                isFeedItemDownloadInProgress(),
                                false
                            ))
                        }
                        is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                            nnPodcast.endEpisodeUpdate(
                                serviceState.episodeId,
                                ::retrieveEpisodeDuration
                            )
                            updateViewState(EpisodeDescriptionViewState.FeedItemDescription(
                                currentViewState.feedItem,
                                currentViewState.feed,
                                podcastEpisode,
                                isFeedItemDownloadInProgress(),
                                false
                            ))
                            feedRepository.updatePlayedMark(nnPodcast.id, true)
                        }
                        else -> {}
                    }
                }
            }
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
