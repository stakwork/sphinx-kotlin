package chat.sphinx.chat_common.ui.viewstate.audio


internal data class AudioMessageState(
    val playState: AudioPlayState,
    val durationMillis: Long,
    val currentMillis: Long
) {
    val progress: Long
        get() = try {
            // duration could be 0
            currentMillis / durationMillis
        } catch (e: ArithmeticException) {
            0L
        }

    val remainingSeconds: Long
        get() = durationMillis - currentMillis
}
