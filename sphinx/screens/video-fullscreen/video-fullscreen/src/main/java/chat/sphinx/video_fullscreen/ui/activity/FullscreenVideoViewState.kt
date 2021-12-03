package chat.sphinx.video_fullscreen.ui.activity

import chat.sphinx.wrapper_common.feed.FeedId
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed  class FullscreenVideoViewState(
    val name: String,
    val duration: Int,
    val videoDimensions: Pair<Int, Int>,
    val currentTime: Int,
    val isPlaying: Boolean,
    val youtubeFeedId: FeedId? = null
) : ViewState<FullscreenVideoViewState>() {
    object Idle : FullscreenVideoViewState(
        "",
        0,
        Pair(0,0),
        0,
        false
    )

    class YoutubeVideo(
        name: String,
        duration: Int,
        videoDimensions: Pair<Int, Int>,
        currentTime: Int,
        isPlaying: Boolean,
        youtubeFeedId: FeedId
    ): FullscreenVideoViewState(
        name,
        duration,
        videoDimensions,
        currentTime,
        isPlaying,
        youtubeFeedId
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