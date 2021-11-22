package chat.sphinx.video_screen.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
internal class VideoScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    repositoryMedia: RepositoryMedia
): BaseViewModel<
        VideoScreenViewState
        >(dispatchers, VideoScreenViewState.Idle)
{
    val args: VideoScreenFragmentArgs by savedStateHandle.navArgs()

    val videoFeedSharedFlow: SharedFlow<Feed?> = flow {
        emitAll(repositoryMedia.getFeedByFeedId(FeedId(args.argFeedId)))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    val feedItemsHolderViewStateFlow: StateFlow<List<FeedItem>> = flow {
        repositoryMedia.getAllFeedItemsFromFeedId(FeedId(args.argFeedId)).collect { feedItems ->
            emit(feedItems.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun episodeSelected(videoEpisode: FeedItem) {
        // TODO: Show video episode detail...
    }
}
