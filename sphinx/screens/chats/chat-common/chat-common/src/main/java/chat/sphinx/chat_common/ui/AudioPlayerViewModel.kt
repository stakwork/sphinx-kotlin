package chat.sphinx.chat_common.ui

import android.app.Application
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.viewstate.audio.AudioMessageState
import chat.sphinx.chat_common.ui.viewstate.audio.AudioPlayState
import chat.sphinx.chat_common.ui.viewstate.messageholder.LayoutState
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal interface AudioPlayerController {
    suspend fun getAudioState(
        audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable
    ): StateFlow<AudioMessageState>?

    fun togglePlayPause(audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment?)
}

@HiltViewModel
internal class AudioPlayerViewModel @Inject constructor(
    private val app: Application,
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
                                    file,
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

    private inner class MediaPlayerHolder {
        private var currentAudio: Pair<File, MutableStateFlow<AudioMessageState>>? = null
        private val mediaPlayer = MediaPlayer().also {
            it.setOnCompletionListener { mp ->
                currentAudio?.let { nnCurrent ->
                    dispatchStateJob?.cancel()
                    nnCurrent.second.value = AudioMessageState(
                        nnCurrent.second.value.file,
                        AudioPlayState.Paused,
                        nnCurrent.second.value.durationMillis,
                        0L,
                    )
                }
            }
        }
        private val publicMethodLock = Mutex()

        suspend fun updateMediaState(state: MutableStateFlow<AudioMessageState>) {
            state.value.file?.let { nnFile ->
                // only handle valid state objects (file is not null)

                publicMethodLock.withLock {
                    currentAudio?.let { nnCurrent ->
                        if (nnCurrent.first == nnFile) {

                            when (nnCurrent.second.value.playState) {
                                AudioPlayState.Error,
                                AudioPlayState.Loading -> { /* no-op */ }
                                AudioPlayState.Paused -> {
                                    playCurrent()
                                }
                                AudioPlayState.Playing -> {
                                    pauseCurrent()
                                }
                            }

                        } else {
                            pauseCurrent()

                            currentAudio = Pair(nnFile, state)

                            state.value = AudioMessageState(
                                state.value.file,
                                AudioPlayState.Loading,
                                state.value.durationMillis,
                                state.value.currentMillis,
                            )

                            mediaPlayer.apply {
                                reset()
                                try {
                                    setDataSource(app.applicationContext, nnFile.toUri())
                                    setOnPreparedListener { mp ->
                                        mp.setOnPreparedListener(null)
                                        mp.seekTo(state.value.currentMillis.toInt())

                                        viewModelScope.launch(mainImmediate) {
                                            if (true /* TODO: Request Audio Focus */) {
                                                playCurrent()
                                            } else {
                                                pauseCurrent()
                                            }
                                        }
                                    }

                                    prepareAsync()
                                } catch (e: IllegalStateException) {

                                    state.value = AudioMessageState(
                                        state.value.file,
                                        AudioPlayState.Error,
                                        state.value.durationMillis,
                                        state.value.currentMillis,
                                    )

                                }
                            }
                        }
                    } ?: run {
                        currentAudio = Pair(nnFile, state)
                        // TODO: Register audio focus listener

                        state.value = AudioMessageState(
                            state.value.file,
                            AudioPlayState.Loading,
                            state.value.durationMillis,
                            state.value.currentMillis,
                        )

                        mediaPlayer.apply {
                            reset()
                            try {
                                setDataSource(app.applicationContext, nnFile.toUri())
                                setOnPreparedListener { mp ->
                                    mp.setOnPreparedListener(null)
                                    mp.seekTo(state.value.currentMillis.toInt())

                                    viewModelScope.launch(mainImmediate) {
                                        if (true /* TODO: Request Audio Focus */) {
                                            playCurrent()
                                        } else {
                                            pauseCurrent()
                                        }
                                    }
                                }

                                prepareAsync()
                            } catch (e: IllegalStateException) {

                                state.value = AudioMessageState(
                                    state.value.file,
                                    AudioPlayState.Error,
                                    state.value.durationMillis,
                                    state.value.currentMillis,
                                )

                            }
                        }
                    }
                }
            }
        }

        suspend fun pauseIfPlaying() {
            publicMethodLock.withLock {
                pauseCurrent()
            }
        }

        private val mediaControllerLock = Mutex()
        private suspend fun pauseCurrent() {
            mediaControllerLock.withLock {
                currentAudio?.let { nnCurrent ->

                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                        dispatchStateJob?.cancel()
                        nnCurrent.second.value = AudioMessageState(
                            nnCurrent.second.value.file,
                            AudioPlayState.Paused,
                            nnCurrent.second.value.durationMillis,
                            mediaPlayer.currentPosition.toLong(),
                        )
                    }

                }
            }
        }

        private suspend fun playCurrent() {
            mediaControllerLock.withLock {
                if (currentAudio != null) {
                    if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                        startDispatchStateJob()
                    }
                }
            }
        }

        fun releaseMediaPlayer() {
            if (currentAudio != null) {
                dispatchStateJob?.cancel()
                mediaPlayer.release()
            }
        }

        private var dispatchStateJob: Job? = null
        private fun startDispatchStateJob() {
            if (dispatchStateJob?.isActive == true) {
                return
            }

            dispatchStateJob = viewModelScope.launch(mainImmediate) {
                while (isActive) {
                    currentAudio?.let { nnCurrent ->
                        nnCurrent.second.value = AudioMessageState(
                            nnCurrent.second.value.file,
                            if (mediaPlayer.isPlaying) {
                                AudioPlayState.Playing
                            } else {
                                AudioPlayState.Paused
                            },
                            nnCurrent.second.value.durationMillis,
                            mediaPlayer.currentPosition.toLong(),
                        )
                    }

                    if (!mediaPlayer.isPlaying) {
                        break
                    } else {
                        delay(500L)
                    }
                }
            }
        }
    }

    private val audioStateCache = AudioStateCache()
    private val mediaPlayerHolder = MediaPlayerHolder()

    override suspend fun getAudioState(
        audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable
    ): StateFlow<AudioMessageState>? {
        return audioStateCache.getOrCreate(audioAttachment.file)?.asStateFlow()
    }

    private var toggleStateJob: Job? = null
    override fun togglePlayPause(audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment?) {
        if (audioAttachment == null) {
            return
        }

        if (audioAttachment !is LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable) {
            return
        }

        if (toggleStateJob?.isActive == true) {
            return
        }

        toggleStateJob = viewModelScope.launch(mainImmediate) {
            val state = audioStateCache.getOrCreate(audioAttachment.file) ?: return@launch
            if (state.value.playState !is AudioPlayState.Loading && state.value.playState !is AudioPlayState.Error) {
                mediaPlayerHolder.updateMediaState(state)
            }
        }
    }

    /**
     * For when user navigates away from screen or sends application to background
     * */
    fun pauseMediaIfPlaying() {
        viewModelScope.launch(mainImmediate) {
            mediaPlayerHolder.pauseIfPlaying()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioStateCache.releaseMetaDataRetriever()
        mediaPlayerHolder.releaseMediaPlayer()
    }
}
