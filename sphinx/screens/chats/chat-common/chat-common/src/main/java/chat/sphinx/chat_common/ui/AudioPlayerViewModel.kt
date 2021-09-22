package chat.sphinx.chat_common.ui

import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import chat.sphinx.chat_common.ui.viewstate.audio.AudioMessageState
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal interface AudioPlayerController {
    suspend fun getAudioState(file: File): StateFlow<AudioMessageState>?
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

        suspend fun getOrCreate(file: File): StateFlow<AudioMessageState>? =
            lock.withLock {
                map[file]?.asStateFlow() ?: run {

                    val durationSeconds: Long = try {

                        metaDataRetriever.setDataSource(file.path)

                        withContext(io) {
                            metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull()
                                ?.div(1000)
                                ?: 1L
                        }
                    } catch (e: IllegalArgumentException) {
                        LOG.e(TAG, "Failed to create AudioMessageState", e)
                        return null
                    }

                    val state = MutableStateFlow(
                        AudioMessageState(
                            file,
                            durationSeconds,
                            0L
                        )
                    )

                    map[file] = state
                    state.asStateFlow()
                }
            }
    }

    private val audioStateCache = AudioStateCache()

    override suspend fun getAudioState(file: File): StateFlow<AudioMessageState>? {
        return audioStateCache.getOrCreate(file)
    }

    override fun onCleared() {
        super.onCleared()
        audioStateCache.releaseMetaDataRetriever()
    }
}
