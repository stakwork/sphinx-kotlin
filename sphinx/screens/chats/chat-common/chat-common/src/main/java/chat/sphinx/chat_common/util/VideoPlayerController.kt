package chat.sphinx.chat_common.util

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.net.toUri
import java.io.File

class VideoPlayerController(
    private val app: Application
) {
    private var mediaPlayer: MediaPlayer? = null

    fun playVideo(file: File) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(app.applicationContext, file.toUri())

                setOnPreparedListener { mp ->
                    start()
                }
                prepareAsync()
            }
        }
    }

    fun clear() {

    }
}