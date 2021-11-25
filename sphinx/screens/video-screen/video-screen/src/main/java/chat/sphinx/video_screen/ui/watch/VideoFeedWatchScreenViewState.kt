package chat.sphinx.video_screen.ui.watch

import io.matthewnelson.concept_views.viewstate.ViewState

@Suppress("ClassName")
internal sealed class VideoFeedWatchScreenViewState(
    val name: String,
    val duration: Int,
    val videoDimensions: Pair<Int, Int>,
    val currentTime: Int,
    val isPlaying: Boolean
): ViewState<VideoFeedWatchScreenViewState>() {
    object Idle : VideoFeedWatchScreenViewState(
        "",
        0,
        Pair(0,0),
        0,
        false
    )

    class VideoTitle(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean
    ): VideoFeedWatchScreenViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying
    )

    class MetaDataLoaded(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean
    ): VideoFeedWatchScreenViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying
    )

    class CurrentTimeUpdate(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean
    ): VideoFeedWatchScreenViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying
    )

    class PausePlayback(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean
    ): VideoFeedWatchScreenViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying
    )

    class ContinuePlayback(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean
    ): VideoFeedWatchScreenViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying
    )

    class CompletePlayback(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean
    ): VideoFeedWatchScreenViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying
    )
}