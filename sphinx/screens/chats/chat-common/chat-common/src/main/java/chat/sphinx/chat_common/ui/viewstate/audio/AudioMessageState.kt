package chat.sphinx.chat_common.ui.viewstate.audio

import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_message.PodcastClip
import java.io.File


internal data class AudioMessageState(
    val messageId: MessageId,
    val messageUUID: MessageUUID?,
    val file: File?,
    val podcastClip: PodcastClip?,
    val playState: AudioPlayState,
    val durationMillis: Long,
    val currentMillis: Long,
) {
    val progress: Long
        get() = try {
            // duration could be 0
            ((currentMillis.toFloat() / durationMillis.toFloat()) * 100).toLong()
        } catch (e: ArithmeticException) {
            0L
        }

    val remainingSeconds: Long
        get() = durationMillis - currentMillis
}
