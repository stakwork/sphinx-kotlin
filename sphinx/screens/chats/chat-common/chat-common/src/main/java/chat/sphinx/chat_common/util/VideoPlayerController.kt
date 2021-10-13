package chat.sphinx.chat_common.util

import android.app.Application
import android.widget.VideoView
import androidx.core.net.toUri
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import java.io.File

class VideoPlayerController(
    app: Application,
    val viewModelScope: CoroutineScope,
    private val updateDurationCallback: (Int) -> Unit,
    private val updateCurrentTimeCallback: (Int) -> Unit,
    dispatchers: CoroutineDispatchers,
) : CoroutineDispatchers by dispatchers {

    private var videoView: VideoView? = null

    fun setVideo(videoView: VideoView) {
        this.videoView = videoView
    }

    fun initializeVideo(videoFile: File) {
        videoView?.apply {
            setOnCompletionListener {
                updateCurrentTimeCallback(0)
            }
            setOnPreparedListener {
                updateDurationCallback(it.duration)
                play()
            }
            // TODO: Handle error...
            setVideoURI(videoFile.toUri())
        }
    }

    private fun play() {
        videoView?.start()
        startDispatchStateJob()
    }

    fun pause() {
        videoView?.pause()
        dispatchStateJob?.cancel()
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