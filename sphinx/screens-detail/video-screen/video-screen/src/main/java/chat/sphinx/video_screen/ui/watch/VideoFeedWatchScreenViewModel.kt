package chat.sphinx.video_screen.ui.watch

import android.net.Uri
import android.widget.VideoView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.video_player_controller.VideoPlayerController
import chat.sphinx.video_screen.ui.VideoFeedScreenViewModel
import chat.sphinx.video_screen.ui.viewstate.PlayingVideoViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val VideoFeedWatchScreenFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

internal inline val VideoFeedWatchScreenFragmentArgs.feedUrl: FeedUrl?
    get() = FeedUrl(argFeedUrl)

internal inline val VideoFeedWatchScreenFragmentArgs.feedId: FeedId?
    get() = FeedId(argFeedId)

@HiltViewModel
internal class VideoFeedWatchScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    feedRepository: FeedRepository,
): VideoFeedScreenViewModel(
    dispatchers,
    chatRepository,
    feedRepository
)
{
    private val args: VideoFeedWatchScreenFragmentArgs by savedStateHandle.navArgs()

    init {
        subscribeToViewStateFlow()

        viewModelScope.launch(mainImmediate) {
            chatRepository.updateChatContentSeenAt(
                getArgChatId()
            )
        }
    }

    open val playingVideoStateContainer: ViewStateContainer<PlayingVideoViewState> by lazy {
        ViewStateContainer(PlayingVideoViewState.Idle)
    }

    private val videoPlayerController: VideoPlayerController by lazy {
        VideoPlayerController(
            viewModelScope = viewModelScope,
            updateIsPlaying = { isPlaying ->
                val currentViewState = playingVideoStateContainer.viewStateFlow.value

                if (isPlaying) {
                    playingVideoStateContainer.updateViewState(
                        PlayingVideoViewState.ContinuePlayback(
                            currentViewState.duration,
                            currentViewState.currentTime,
                            currentViewState.videoDimensions,
                            isPlaying
                        )
                    )
                } else {
                    playingVideoStateContainer.updateViewState(
                        PlayingVideoViewState.PausePlayback(
                            currentViewState.duration,
                            currentViewState.currentTime,
                            currentViewState.videoDimensions,
                            isPlaying
                        )
                    )
                }
            },
            updateMetaDataCallback = { duration, videoWidth, videoHeight ->
                val currentViewState = playingVideoStateContainer.viewStateFlow.value

                playingVideoStateContainer.updateViewState(
                    PlayingVideoViewState.MetaDataLoaded(
                        duration,
                        currentViewState.currentTime,
                        Pair(videoWidth, videoHeight),
                        currentViewState.isPlaying
                    )
                )
            },
            updateCurrentTimeCallback = { currentTime ->
                val currentViewState = playingVideoStateContainer.viewStateFlow.value

                playingVideoStateContainer.updateViewState(
                    PlayingVideoViewState.CurrentTimeUpdate(
                        currentViewState.duration,
                        currentTime,
                        currentViewState.videoDimensions,
                        currentViewState.isPlaying
                    )
                )
            },
            completePlaybackCallback = {
                val currentViewState = playingVideoStateContainer.viewStateFlow.value

                playingVideoStateContainer.updateViewState(
                    PlayingVideoViewState.CompletePlayback(
                        currentViewState.duration,
                        currentTime = 0,
                        currentViewState.videoDimensions,
                        isPlaying = false
                    )
                )
            },
            dispatchers
        )
    }

    fun initializeVideo(
        videoUri: Uri,
        videoDuration: Int?
    ) {
        if (videoDuration != null && videoDuration > 0) {
            videoPlayerController.initializeVideo(
                videoUri,
                videoDuration * 1000
            )
        } else {
            videoPlayerController.initializeVideo(videoUri)
        }
    }

    fun setVideoView(videoView: VideoView) {
        videoPlayerController.setVideo(videoView)
    }

    fun togglePlayPause() {
        videoPlayerController.togglePlayPause()
    }

    fun seekTo(progress: Int) {
        videoPlayerController.seekTo(progress)
    }

    override fun getArgChatId(): ChatId {
        return args.chatId
    }

    override fun getArgFeedUrl(): FeedUrl? {
        return args.feedUrl
    }

    override fun getArgFeedId(): FeedId? {
        return args.feedId
    }
}
