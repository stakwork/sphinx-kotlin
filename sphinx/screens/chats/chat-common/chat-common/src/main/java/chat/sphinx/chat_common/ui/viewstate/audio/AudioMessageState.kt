package chat.sphinx.chat_common.ui.viewstate.audio


internal data class AudioMessageState(
    val playState: AudioPlayState,
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
