package chat.sphinx.chat_common.ui.activity

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed  class FullscreenVideoViewState(
    val name: String,
    val duration: Int,
    val videoDimensions: Pair<Int, Int>,
    val currentTime: Int,
    val isPlaying: Boolean
) : ViewState<FullscreenVideoViewState>() {
    object Idle : FullscreenVideoViewState(
        "",
        0,
        Pair(0,0),
        0,
        false
    )

    class VideoMessage(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean
    ): FullscreenVideoViewState(
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
    ): FullscreenVideoViewState(
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
    ): FullscreenVideoViewState(
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
    ): FullscreenVideoViewState(
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
    ): FullscreenVideoViewState(
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
    ): FullscreenVideoViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying
    )
}