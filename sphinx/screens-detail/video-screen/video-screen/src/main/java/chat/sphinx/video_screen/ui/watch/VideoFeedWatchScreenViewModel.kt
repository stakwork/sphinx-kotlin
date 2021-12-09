package chat.sphinx.video_screen.ui.watch

import android.net.Uri
import android.widget.VideoView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.video_player_controller.VideoPlayerController
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import chat.sphinx.video_screen.ui.VideoFeedScreenViewModel
import chat.sphinx.video_screen.ui.viewstate.PlayingVideoViewState
import chat.sphinx.video_screen.ui.viewstate.SelectedVideoViewState
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.message.MessageId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val VideoFeedWatchScreenFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

internal inline val VideoFeedWatchScreenFragmentArgs.feedUrl: FeedUrl?
    get() = FeedUrl(argFeedUrl)

@HiltViewModel
internal class VideoFeedWatchScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val videoScreenNavigator: VideoScreenNavigator,
    private val chatRepository: ChatRepository,
): VideoFeedScreenViewModel(
    dispatchers,
    chatRepository
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

    val videoPlayerController: VideoPlayerController by lazy {
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

    @Synchronized
    fun goToFullscreenVideo() {
        val viewState = selectedVideoStateContainer.value

        if (viewState is SelectedVideoViewState.VideoSelected) {
            viewModelScope.launch(mainImmediate) {
                videoScreenNavigator.toFullScreenVideoActivity(
                    messageId = MessageId(-1L),
                    videoFilepath = null,
                    feedId = viewState.id,
                    currentTime = playingVideoStateContainer.viewStateFlow.value.currentTime
                )
            }
        }
    }
}
