package chat.sphinx.video_player_controller

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*

class VideoPlayerController(
    private val viewModelScope: CoroutineScope,
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

    fun initializeVideo(
        videoUri: Uri,
        videoDuration: Int? = null
    ) {
        videoView?.apply {
            setOnCompletionListener {
                completePlaybackCallback()
            }
            setOnPreparedListener {
                updateMetaDataCallback(
                    videoDuration ?: it.duration,
                    it.videoWidth,
                    it.videoHeight
                )
                play()
            }

            setVideoURI(videoUri)
        }
    }

    private fun play() {
        videoView?.start()
        startDispatchStateJob()
        updateIsPlaying(true)
    }

    fun seekTo(progress: Int) {
        videoView?.seekTo(progress)
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
                    if (video.isPlaying) {
                        updateCurrentTimeCallback(video.currentPosition)

                        delay(250L)
                    } else {
                        break
                    }
                }
            }
        }
    }
}

fun Uri.getMediaDuration(
    isLocalFile: Boolean
): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        if (Build.VERSION.SDK_INT >= 14 && !isLocalFile) {
            retriever.setDataSource(this.toString(), HashMap<String, String>())
        } else {
            retriever.setDataSource(this.toString())
        }
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        duration?.toLongOrNull() ?: 0
    } catch (exception: Exception) {
        0
    }
}