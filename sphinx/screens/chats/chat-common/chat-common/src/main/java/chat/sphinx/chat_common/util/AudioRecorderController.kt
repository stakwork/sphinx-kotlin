package chat.sphinx.chat_common.util

import android.content.Context
import androidx.navigation.NavArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

internal class AudioRecorderController<ARGS : NavArgs>(
    private val viewModelScope: CoroutineScope,
    private val mediaCacheHandler: MediaCacheHandler,
    private val updateDurationCallback: (Long) -> Unit,
    private val dispatchers: CoroutineDispatchers,
): CoroutineDispatchers by dispatchers {
    private val lock = Mutex()
    var recorderAndFile: Pair<WavRecorder, File>? = null

    val recordingTempFile: File?
        get() = recorderAndFile?.second

    private var dispatchStateJob: Job? = null
    fun startAudioRecording(context: Context) {
        // TODO: Pause other media playing
        viewModelScope.launch(dispatchers.mainImmediate) {
            lock.withLock {
                if (dispatchStateJob?.isActive == true) {
                    return@launch
                }

                WavRecorder(context).apply {
                    val recordingTempFile = mediaCacheHandler.createAudioFile(
                        AUDIO_FORMAT_EXTENSION
                    )
                    recorderAndFile = Pair(
                        this,
                        recordingTempFile
                    )

                    startRecording(recordingTempFile)

                    dispatchStateJob = viewModelScope.launch(mainImmediate) {
                        var duration = 0L
                        while (isActive) {
                            updateDurationCallback(duration)
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

        }
    }

    fun stopAudioRecording() {
        recorderAndFile?.first?.stopRecording()
        cancelDispatchJob()
    }

    fun stopAndDeleteAudioRecording() {
        recorderAndFile?.first?.stopRecording()
        clear()
    }

    fun isRecording(): Boolean {
        return recorderAndFile != null
    }

    private fun cancelDispatchJob() {
        dispatchStateJob?.cancel()
        dispatchStateJob = null
    }

    fun clear() {
        viewModelScope.launch(mainImmediate) {
            lock.withLock {
                cancelDispatchJob()
                recorderAndFile = null
            }
        }
    }

    companion object {
        const val AUDIO_FORMAT_EXTENSION = "wav"
        const val AUDIO_FORMAT_MIME_TYPE = "audio/wav"
    }
}