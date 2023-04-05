package chat.sphinx.episode_description.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
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
        >(dispatchers, EpisodeDescriptionViewState.Idle) {
    private val args: EpisodeDescriptionFragmentArgs by savedStateHandle.navArgs()

    init {
        getFeedItem(args.argFeedId.toFeedId())
    }

    private fun getFeedItem(feedId: FeedId?) {
        viewModelScope.launch(mainImmediate) {
            feedId?.let { nnFeedId ->
                feedRepository.getFeedItemById(nnFeedId).collect { feedItem ->
                    feedItem?.let { mmFeedItem ->
                        val feed = feedRepository.getFeedById(feedItem.feedId).firstOrNull()
                        val podcastEpisode = feedRepository.getPodcastById(feedItem.feedId).firstOrNull()?.getEpisodeWithId(feedItem.id.value)
                        updateViewState(EpisodeDescriptionViewState.FeedItemDescription(mmFeedItem, feed, podcastEpisode, isFeedItemDownloadInProgress()))
                    }
                }
            }
        }
    }

    fun isFeedItemDownloadInProgress(): Boolean {
        (currentViewState as? EpisodeDescriptionViewState.FeedItemDescription)?.feedItem?.id.let { feedItemId  ->
            return repositoryMedia.inProgressDownloadIds().contains(feedItemId)
        }
    }

    fun isEpisodeSoundPlaying(episode: PodcastEpisode): Boolean {
        return episode.playing && mediaPlayerServiceController.getPlayingContent()?.third == true
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
            updateViewState(EpisodeDescriptionViewState.FeedItemDescription(currentViewState.feedItem, currentViewState.feed, podcastEpisode, isFeedItemDownloadInProgress()))
        }
    }
}

//    fun downloadMediaByItemId(feedId: FeedId) {
//        viewModelScope.launch(mainImmediate) {
//            feedRepository.getFeedItemById(feedId).firstOrNull()?.let { feedItem ->
//                repositoryMedia.downloadMediaIfApplicable(feedItem) { localFile ->
//
//                    _feedItemDetailStateFlow.value = _feedItemDetailStateFlow.value?.copy(
//                        downloaded = true,
//                        isDownloadInProgress = false
//                    )
//
//                    getPodcastFeed()?.let { nnPodcast ->
//                        nnPodcast.getEpisodeWithId(feedId.value)?.let { episode ->
//                            episode.localFile = localFile
//                        }
//                    }
//
//                    if (feedItemDetailsViewStateContainer.value is FeedItemDetailsViewState.Open) {
//                        feedItemDetailsViewStateContainer.updateViewState(
//                            FeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
//                        )
//                    }
//
//                    forceListReload()
//                }
//            }
//
//            val isFeedItemDownloadInProgress = repositoryMedia.inProgressDownloadIds()
//                .contains(feedId)
//
//            _feedItemDetailStateFlow.value = _feedItemDetailStateFlow.value?.copy(
//                isDownloadInProgress = isFeedItemDownloadInProgress
//            )
//
//            feedItemDetailsViewStateContainer.updateViewState(
//                FeedItemDetailsViewState.Open(feedItemDetailStateFlow.value)
//            )
//
//            forceListReload()
//        }
//    }


