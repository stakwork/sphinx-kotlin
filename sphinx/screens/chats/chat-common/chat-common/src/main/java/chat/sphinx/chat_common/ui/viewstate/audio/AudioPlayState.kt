package chat.sphinx.chat_common.ui.viewstate.audio

internal sealed class AudioPlayState {
    object Error: AudioPlayState()
    object Loading: AudioPlayState()
    object Playing: AudioPlayState()
    object Paused: AudioPlayState()
}
