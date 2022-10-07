package chat.sphinx.video_screen.ui.watch

import android.content.ContentValues
import android.util.Log
import android.widget.Toast
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val YOUTUBE_VIDEO_ID = "Evfe8GEn33w"
const val YOUTUBE_PLAYLIST = "UCU3jy5C8MB-JvSw_86SFV2w"
const val GOOGLE_API_KEY = "AIzaSyCgjzFVYxS_a9bRrcUtUwISE9HpjzmcpF0"

class YoutubeApi : YouTubeBaseActivity(), YouTubePlayer.OnInitializedListener {

    lateinit var player: YouTubePlayer

    override fun onInitializationSuccess(
        provider: YouTubePlayer.Provider?,
        youTubePlayer: YouTubePlayer?,
        wasRestored: Boolean
    ) {
        Log.d(ContentValues.TAG, "onInitializationSuccess: provider is ${provider?.javaClass}")
        Log.d(ContentValues.TAG, "onInitializationSuccess: youTubePlayer is ${youTubePlayer?.javaClass}")
        Toast.makeText(this, "Initialized Youtube Player successfully", Toast.LENGTH_SHORT).show()

        youTubePlayer?.setPlayerStateChangeListener(playerStateChangeListener)
        youTubePlayer?.setPlaybackEventListener(playbackEventListener)

        if (!wasRestored) {
            youTubePlayer?.let {
                player = it
            }
        }
    }

    override fun onInitializationFailure(
        provider: YouTubePlayer.Provider?,
        youTubeInitializationResult: YouTubeInitializationResult?
    ) {
        val REQUEST_CODE = 0

        if (youTubeInitializationResult?.isUserRecoverableError == true) {
            youTubeInitializationResult.getErrorDialog(this, REQUEST_CODE).show()
        } else {
            val errorMessage =
                "There was an error initializing the YoutubePlayer ($youTubeInitializationResult)"
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private val playbackEventListener = object : YouTubePlayer.PlaybackEventListener {
        override fun onSeekTo(p0: Int) {
        }

        override fun onBuffering(p0: Boolean) {
        }

        override fun onPlaying() {
            Toast.makeText(this@YoutubeApi, "Good, video is playing ok", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onStopped() {
            Toast.makeText(this@YoutubeApi, "Video has stopped", Toast.LENGTH_SHORT).show()
        }

        override fun onPaused() {
            Toast.makeText(this@YoutubeApi, "Video has paused", Toast.LENGTH_SHORT).show()
        }
    }

    private val playerStateChangeListener = object : YouTubePlayer.PlayerStateChangeListener {
        override fun onAdStarted() {
            Toast.makeText(
                this@YoutubeApi,
                "Click Ad now, make the video creator rich!",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onLoading() {
        }

        override fun onVideoStarted() {
            Toast.makeText(this@YoutubeApi, "Video has started", Toast.LENGTH_SHORT).show()
        }

        override fun onLoaded(p0: String?) {
        }

        override fun onVideoEnded() {
            Toast.makeText(
                this@YoutubeApi,
                "Congratulations! You've completed another video.",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onError(p0: YouTubePlayer.ErrorReason?) {
        }
    }
}