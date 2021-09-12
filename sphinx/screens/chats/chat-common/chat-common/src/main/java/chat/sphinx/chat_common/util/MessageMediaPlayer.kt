package chat.sphinx.chat_common.util

import android.media.MediaPlayer
import android.os.CountDownTimer
import chat.sphinx.chat_common.databinding.LayoutMessageTypeAttachmentAudioBinding
import kotlinx.coroutines.CoroutineScope

class MessageMediaPlayer: MediaPlayer() {
    var filePath: String? = null
    var countDownTimer: CountDownTimer? = null

    fun load(filePath: String) {
        this.filePath = filePath
        setDataSource(
            filePath
        )
        prepare()
    }

    override fun reset() {
        super.reset()
        countDownTimer?.cancel()
        // TODO: Reset the UI as well...
    }
    companion object {
        fun setBubbleAudioAttachment(
            lifecycleScope: CoroutineScope,
            mediaMessageTypeAttachmentAudioBinding: LayoutMessageTypeAttachmentAudioBinding,
            layoutMessageAudioAttachment: LayoutMessageTypeAttachmentAudioBinding,
            audioFilePath: String
        ) {

            layoutMessageAudioAttachment.apply {

            }
        }
    }
}