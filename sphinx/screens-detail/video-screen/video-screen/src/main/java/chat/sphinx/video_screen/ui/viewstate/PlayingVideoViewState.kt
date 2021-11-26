package chat.sphinx.video_screen.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PlayingVideoViewState(
    val duration: Int,
    val currentTime: Int,
    val videoDimensions: Pair<Int, Int>,
    val isPlaying: Boolean
): ViewState<PlayingVideoViewState>() {
    object Idle : PlayingVideoViewState(
        0,
        0,
        Pair(0,0),
        false
    )

    class MetaDataLoaded(
        duration: Int,
        currentTime: Int,
        videoDimensions: Pair<Int, Int>,
        isPlaying: Boolean
    ): PlayingVideoViewState(
        duration,
        currentTime,
        videoDimensions,
        isPlaying
    )

    class CurrentTimeUpdate(
        duration: Int,
        currentTime: Int,
        videoDimensions: Pair<Int, Int>,
        isPlaying: Boolean
    ): PlayingVideoViewState(
        duration,
        currentTime,
        videoDimensions,
        isPlaying
    )

    class PausePlayback(
        duration: Int,
        currentTime: Int,
        videoDimensions: Pair<Int, Int>,
        isPlaying: Boolean
    ): PlayingVideoViewState(
        duration,
        currentTime,
        videoDimensions,
        isPlaying
    )

    class ContinuePlayback(
        duration: Int,
        currentTime: Int,
        videoDimensions: Pair<Int, Int>,
        isPlaying: Boolean
    ): PlayingVideoViewState(
        duration,
        currentTime,
        videoDimensions,
        isPlaying
    )

    class CompletePlayback(
        duration: Int,
        currentTime: Int,
        videoDimensions: Pair<Int, Int>,
        isPlaying: Boolean
    ): PlayingVideoViewState(
        duration,
        currentTime,
        videoDimensions,
        isPlaying
    )
}
