package chat.sphinx.common_player.ui

import android.webkit.JavascriptInterface

class YoutubePlayerJavaScriptInterface(private val listener: VideoStateListener) {

    @JavascriptInterface
    fun onVideoReady() {
        listener.onVideoReady()
    }

    @JavascriptInterface
    fun onVideoStateChange(state: String) {
        when(state) {
            "UNSTARTED" -> listener.onVideoUnstarted()
            "ENDED" -> listener.onVideoEnded()
            "PLAYING" -> listener.onVideoPlaying()
            "PAUSED" -> listener.onVideoPaused()
            "BUFFERING" -> listener.onVideoBuffering()
            "CUED" -> listener.onVideoCued()
        }
    }

    @JavascriptInterface
    fun onError(error: String) {
        listener.onVideoError(error)
    }

    @JavascriptInterface
    fun onPlaybackQualityChange(quality: String) {
        listener.onPlaybackQualityChange(quality)
    }

    @JavascriptInterface
    fun onPlaybackRateChange(rate: String) {
        listener.onPlaybackRateChange(rate)
    }

    @JavascriptInterface
    fun onVideoSeek(time: Int) {
        listener.onVideoSeek(time)
    }
}

interface VideoStateListener {
    fun onVideoReady()
    fun onVideoUnstarted()
    fun onVideoEnded()
    fun onVideoPlaying()
    fun onVideoPaused()
    fun onVideoBuffering()
    fun onVideoCued()
    fun onVideoError(error: String)
    fun onPlaybackQualityChange(quality: String)
    fun onPlaybackRateChange(rate: String)
    fun onVideoSeek(time: Int)

}
