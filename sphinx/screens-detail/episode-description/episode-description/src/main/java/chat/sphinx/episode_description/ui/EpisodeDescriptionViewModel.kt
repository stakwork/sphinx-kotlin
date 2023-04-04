package chat.sphinx.episode_description.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.toFeedId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
                        updateViewState(EpisodeDescriptionViewState.FeedItemDescription(mmFeedItem, feed, podcastEpisode))
                    }
                }
            }
        }
    }



}