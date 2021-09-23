package chat.sphinx.chat_common.ui

import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.audio.AudioMessageState
import chat.sphinx.chat_common.ui.viewstate.audio.AudioPlayState
import chat.sphinx.chat_common.ui.viewstate.messageholder.LayoutState
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal interface AudioPlayerController {
    suspend fun getAudioState(
        audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable
    ): StateFlow<AudioMessageState>?
}

@HiltViewModel
internal class AudioPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val LOG: SphinxLogger,
): ViewModel(), AudioPlayerController, CoroutineDispatchers by dispatchers {

    companion object {
        const val TAG = "AudioPlayerViewModel"
    }

    private inner class AudioStateCache {
        private val lock = Mutex()
        private val map = mutableMapOf<File, MutableStateFlow<AudioMessageState>>()

        private val metaDataRetriever = MediaMetadataRetriever()
        fun releaseMetaDataRetriever() {
            metaDataRetriever.release()
        }

        suspend fun getOrCreate(file: File): MutableStateFlow<AudioMessageState>? {
            var response: MutableStateFlow<AudioMessageState>? = null

            viewModelScope.launch(mainImmediate) {

                lock.withLock {

                    map[file]?.let { state -> response = state } ?: run {

                        // create new stateful object
                        val durationMillis: Long? = try {

                            metaDataRetriever.setDataSource(file.path)

                            withContext(io) {
                                metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    ?.toLongOrNull()
                                    ?: 1000L
                            }
                        } catch (e: IllegalArgumentException) {
                            LOG.e(TAG, "Failed to create AudioMessageState", e)
                            null
                        }

                        if (durationMillis != null) {
                            val state = MutableStateFlow(
                                AudioMessageState(
                                    AudioPlayState.Paused,
                                    durationMillis,
                                    0L
                                )
                            )

                            map[file] = state
                            response = state
                        }
                    }
                }
            }.join()

            return response
        }
    }

    private val audioStateCache = AudioStateCache()

    override suspend fun getAudioState(
        audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable
    ): StateFlow<AudioMessageState>? {
        return audioStateCache.getOrCreate(audioAttachment.file)?.asStateFlow()
    }

    override fun onCleared() {
        super.onCleared()
        audioStateCache.releaseMetaDataRetriever()
    }
}
