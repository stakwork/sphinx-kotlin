package chat.sphinx.chat_common.util

import android.media.MediaRecorder
import androidx.navigation.NavArgs
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.footer.FooterViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.*
import java.io.File

class AudioRecorderController<ARGS : NavArgs>(
    private val viewModel: ChatViewModel<ARGS>,
    private val viewModelScope: CoroutineScope,
    val mediaCacheHandler: MediaCacheHandler,
    dispatchers: CoroutineDispatchers,
): CoroutineDispatchers by dispatchers {
    var recordingTempFile: File? = null
    private var recorder: MediaRecorder? = null

    private var dispatchStateJob: Job? = null
    fun startAudioRecording() {
        if (dispatchStateJob?.isActive == true) {
            return
        }

        recorder = MediaRecorder().apply {
            recordingTempFile  = mediaCacheHandler.createAudioFile(AUDIO_FORMAT_EXTENSION)

            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(recordingTempFile?.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)

            prepare()

            start()

            dispatchStateJob = viewModelScope.launch(mainImmediate) {
                var duration = 0L
                while (isActive) {
                    viewModel.updateFooterViewState(
                        FooterViewState.RecordingAudioAttachment(duration)
                    )
                    if (!isRecording()) {
                        break
                    } else {
                        duration += 250L
                        delay(250L)
                    }
                }
            }
        }
    }

    fun stopAudioRecording() {
        recorder?.stop()
        cancelDispatchJob()
    }

    fun stopAndDeleteAudioRecording() {
        recorder?.stop()
        cancelDispatchJob()
        // TODO: Delete audio recording
        clear()
    }

    fun isRecording(): Boolean {
        return recordingTempFile != null
    }

    private fun cancelDispatchJob() {
        dispatchStateJob?.cancel()
        dispatchStateJob = null
    }

    fun clear() {
        dispatchStateJob?.cancel()
        recorder?.release()

        recorder = null
        recordingTempFile = null
    }

    companion object {
        const val AUDIO_FORMAT_EXTENSION = "m4a"
        const val AUDIO_FORMAT_MIME_TYPE = "audio/m4a"
    }
}