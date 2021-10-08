package chat.sphinx.chat_common.util

import android.media.MediaRecorder
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
    var recorderAndFile: Pair<MediaRecorder, File>? = null

    val recordingTempFile: File?
        get() = recorderAndFile?.second

    private var dispatchStateJob: Job? = null
    fun startAudioRecording() {
        // TODO: Pause other media playing
        viewModelScope.launch(dispatchers.mainImmediate) {
            lock.withLock {
                if (dispatchStateJob?.isActive == true) {
                    return@launch
                }

                MediaRecorder().apply {
                    val recordingTempFile = mediaCacheHandler.createAudioFile(
                        AUDIO_FORMAT_EXTENSION
                    ).also { file ->
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setOutputFile(file.absolutePath)
                        setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                    }
                    recorderAndFile = Pair(
                        this,
                        recordingTempFile
                    )

                    prepare()

                    start()

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
        recorderAndFile?.first?.stop()
        cancelDispatchJob()
    }

    fun stopAndDeleteAudioRecording() {
        recorderAndFile?.first?.stop()
        // TODO: Delete audio recording
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
                recorderAndFile?.first?.release()

                recorderAndFile = null
            }
        }
    }

    companion object {
        const val AUDIO_FORMAT_EXTENSION = "m4a"
        const val AUDIO_FORMAT_MIME_TYPE = "audio/m4a"
    }
}