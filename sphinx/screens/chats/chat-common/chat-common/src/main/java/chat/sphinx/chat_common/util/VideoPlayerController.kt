package chat.sphinx.chat_common.util

import android.content.Context
import android.util.AttributeSet
import android.widget.MediaController
import android.widget.VideoView
import androidx.core.net.toUri
import java.io.File

class VideoPlayerController : MediaController {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, useFastForward: Boolean) : super(context, useFastForward) {}
    constructor(context: Context) : super(context) {}

    private var videoView: VideoView? = null

    fun setVideo(videoView: VideoView) {
        setMediaPlayer(videoView)
//        videoView.setMediaController(this)

        this.videoView = videoView
    }

    fun initializeVideo(videoFile: File) {
        videoView?.setVideoURI(videoFile.toUri())
        videoView?.start()
    }

    fun pause() {
        videoView?.pause()
    }

    fun stop() {
        videoView?.stopPlayback()
    }

    fun togglePlayPause() {
        if (videoView?.isPlaying == true) {
            videoView?.pause()
        } else {
            videoView?.start()
        }
    }

    fun clear() {
        videoView?.stopPlayback()
    }
}