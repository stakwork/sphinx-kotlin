package chat.sphinx.chat_common.ui.viewstate.audio

import java.io.File

internal data class AudioMessageState(
    val file: File,
    val durationSeconds: Long,
    val currentSeconds: Long
) {
    val progress: Long
        get() = try {
            // duration could be 0
            currentSeconds / durationSeconds
        } catch (e: ArithmeticException) {
            0L
        }
}
