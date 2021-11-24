package chat.sphinx.video_screen.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.video_screen.ui.VideoFeedViewModel
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
internal class VideoFeedDetailScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    repositoryMedia: RepositoryMedia
): BaseViewModel<
        VideoFeedDetailScreenViewState
        >(dispatchers, VideoFeedDetailScreenViewState.Idle), VideoFeedViewModel
{
    val args: VideoFeedDetailScreenFragmentArgs by savedStateHandle.navArgs()

    override val videoFeedSharedFlow: SharedFlow<Feed?> = flow {
        emitAll(repositoryMedia.getFeedByFeedId(FeedId(args.argFeedId)))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    override val feedItemsHolderViewStateFlow: StateFlow<List<FeedItem>> = flow {
        repositoryMedia.getAllFeedItemsFromFeedId(FeedId(args.argFeedId)).collect { feedItems ->
            emit(feedItems.toList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
}
