package chat.sphinx.chat_common.ui

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import chat.sphinx.chat_common.databinding.LayoutMessageTypeAttachmentAudioBinding
import chat.sphinx.chat_common.ui.viewstate.messageholder.toTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
internal class MediaPlayerViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers
): ViewModel()
{
    private val mutex = Mutex()
    private val mediaPlaybackStateCache: MutableMap<String, AudioPlaybackStateData> = HashMap()
    private val loadingMediaPlayer = MediaPlayer()

    suspend fun loadMedia(filePath: String, layoutMessageAudioAttachment: LayoutMessageTypeAttachmentAudioBinding) {
        layoutMessageAudioAttachment.apply {
            if (!mediaPlaybackStateCache.containsKey(filePath)) {
                mutex.withLock {
                    loadingMediaPlayer.apply {
                        try {
                            reset()
                            setDataSource(
                                filePath
                            )

                            prepare()
                            mediaPlaybackStateCache[filePath] = AudioPlaybackStateData(
                                duration = duration,
                                currentPosition = currentPosition,
                            )
                        } catch (e: IOException) {

                        }
                    }
                }
            }

            val audioPlaybackStateData = mediaPlaybackStateCache[filePath]

            if (audioPlaybackStateData != null) {
                seekBarAttachmentAudio.max = audioPlaybackStateData.duration
                seekBarAttachmentAudio.progress = audioPlaybackStateData.currentPosition
                textViewAttachmentAudioRemainingDuration.text = audioPlaybackStateData.duration.toLong().toTimestamp()
                progressBarAttachmentAudioFileLoading.gone
                textViewAttachmentPlayPauseButton.visible
            } else {
                progressBarAttachmentAudioFileLoading.gone
                textViewAttachmentAudioFailure.visible
            }
        }
    }

    fun updateCurrentPosition(filePath: String, currentPosition: Int) {
        mediaPlaybackStateCache[filePath]?.let { audioPlaybackStateData ->
            audioPlaybackStateData.currentPosition = currentPosition
        }
    }

    fun getCurrentPosition(filePath: String): Int? = mediaPlaybackStateCache[filePath]?.currentPosition

    override fun onCleared() {
        super.onCleared()
        loadingMediaPlayer.release()
    }

    internal class AudioPlaybackStateData(
        val duration: Int,
        var currentPosition: Int,
    )
}
