package chat.sphinx.chat_common.util

import android.widget.VideoView
import androidx.core.net.toUri
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import java.io.File

class VideoPlayerController(
    val viewModelScope: CoroutineScope,
    private val updateIsPlaying: (Boolean) -> Unit,
    private val updateMetaDataCallback: (Int, Int, Int) -> Unit,
    private val updateCurrentTimeCallback: (Int) -> Unit,
    private val completePlaybackCallback: () -> Unit,
    dispatchers: CoroutineDispatchers,
) : CoroutineDispatchers by dispatchers {

    private var videoView: VideoView? = null

    fun setVideo(videoView: VideoView) {
        this.videoView = videoView
    }

    fun initializeVideo(videoFile: File) {
        videoView?.apply {
            setOnCompletionListener {
                completePlaybackCallback()
            }
            setOnPreparedListener {
                it.videoHeight
                updateMetaDataCallback(
                    it.duration,
                    it.videoWidth,
                    it.videoHeight
                )
                play()
            }

            setVideoURI(videoFile.toUri())
        }
    }

    private fun play() {
        videoView?.start()
        startDispatchStateJob()
        updateIsPlaying(true)
    }

    fun pause() {
        videoView?.pause()
        dispatchStateJob?.cancel()
        updateIsPlaying(false)
    }

    fun togglePlayPause() {
        if (videoView?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    fun clear() {
        videoView?.stopPlayback()
        dispatchStateJob?.cancel()
    }

    private var dispatchStateJob: Job? = null
    private fun startDispatchStateJob() {
        if (dispatchStateJob?.isActive == true) {
            return
        }

        dispatchStateJob = viewModelScope.launch(mainImmediate) {
            videoView?.let { video ->
                while (isActive) {
                    updateCurrentTimeCallback(video.currentPosition)

                    if (video.isPlaying) {
                        delay(250L)
                    } else {
                        break
                    }
                }
            }
        }
    }
}