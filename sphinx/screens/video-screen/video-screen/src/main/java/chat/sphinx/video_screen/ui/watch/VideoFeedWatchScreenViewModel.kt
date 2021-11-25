package chat.sphinx.video_screen.ui.watch

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.video_player_controller.VideoPlayerController
import chat.sphinx.video_screen.ui.VideoFeedViewModel
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class VideoFeedWatchScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    repositoryMedia: RepositoryMedia
): BaseViewModel<
        VideoFeedWatchScreenViewState
        >(dispatchers, VideoFeedWatchScreenViewState.Idle), VideoFeedViewModel
{
    val args: VideoFeedWatchScreenFragmentArgs by savedStateHandle.navArgs()

    internal val videoPlayerController: VideoPlayerController by lazy {
        VideoPlayerController(
            viewModelScope = viewModelScope,
            updateIsPlaying = { isPlaying ->
                if (isPlaying) {
                    updateViewState(
                        VideoFeedWatchScreenViewState.ContinuePlayback(
                            currentViewState.name,
                            currentViewState.duration,
                            currentViewState.videoDimensions,
                            currentViewState.currentTime,
                            isPlaying
                        )
                    )
                } else {
                    updateViewState(
                        VideoFeedWatchScreenViewState.PausePlayback(
                            currentViewState.name,
                            currentViewState.duration,
                            currentViewState.videoDimensions,
                            currentViewState.currentTime,
                            isPlaying
                        )
                    )
                }
            },
            updateMetaDataCallback = { duration, videoWidth, videoHeight ->
                updateViewState(
                    VideoFeedWatchScreenViewState.MetaDataLoaded(
                        currentViewState.name,
                        duration,
                        Pair(videoWidth, videoHeight),
                        currentViewState.currentTime,
                        currentViewState.isPlaying
                    )
                )
            },
            updateCurrentTimeCallback = { currentTime ->
                updateViewState(
                    VideoFeedWatchScreenViewState.CurrentTimeUpdate(
                        currentViewState.name,
                        currentViewState.duration,
                        currentViewState.videoDimensions,
                        currentTime,
                        currentViewState.isPlaying
                    )
                )
            },
            completePlaybackCallback = {
                updateViewState(
                    VideoFeedWatchScreenViewState.CompletePlayback(
                        currentViewState.name,
                        currentViewState.duration,
                        currentViewState.videoDimensions,
                        currentTime = 0,
                        isPlaying = false
                    )
                )
            },
            dispatchers
        )
    }


    override val videoFeedSharedFlow: SharedFlow<Feed?> = flow {
        videoItemSharedFlow.collect { feedItem ->
            if (feedItem != null) {
                emitAll(repositoryMedia.getFeedByFeedId(feedItem.feedId))
            }
        }

    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    override val feedItemsHolderViewStateFlow: StateFlow<List<FeedItem>> = flow {
        videoItemSharedFlow.collect { feedItem ->
            if (feedItem != null) {
                repositoryMedia.getAllFeedItemsFromFeedId(feedItem.feedId).collect { feedItems ->
                    emit(feedItems.toList())
                }
            }
        }

    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val videoItemSharedFlow: SharedFlow<FeedItem?> = flow {
        emitAll(repositoryMedia.getFeedItemById(FeedId(args.argFeedItemId)))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    fun initializeVideo() {
        viewModelScope.launch(mainImmediate) {
            videoItemSharedFlow.collect {
                it?.let { feedItem ->
                    updateViewState(
                        VideoFeedWatchScreenViewState.VideoTitle(
                            feedItem.titleToShow,
                            currentViewState.duration,
                            currentViewState.videoDimensions,
                            currentViewState.currentTime,
                            currentViewState.isPlaying
                        )
                    )

                    videoPlayerController.initializeVideo(feedItem.enclosureUrl.value.toUri())
                }
            }
        }
    }
}
