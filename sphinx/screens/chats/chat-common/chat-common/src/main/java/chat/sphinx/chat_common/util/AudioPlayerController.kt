package chat.sphinx.chat_common.util

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.net.toUri
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import chat.sphinx.chat_common.ui.viewstate.audio.AudioMessageState
import chat.sphinx.chat_common.ui.viewstate.audio.AudioPlayState
import chat.sphinx.chat_common.ui.viewstate.messageholder.LayoutState
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.wrapper_common.message.MessageId
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

internal interface AudioPlayerController {
    suspend fun getAudioState(
        audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable
    ): StateFlow<AudioMessageState>?

    suspend fun getAudioState(
        podcastClip: LayoutState.Bubble.ContainerSecond.PodcastClip
    ): StateFlow<AudioMessageState>?

    fun togglePlayPause(audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment?)

    fun togglePlayPause(podcastClip: LayoutState.Bubble.ContainerSecond.PodcastClip?)

    fun pauseMediaIfPlaying()
}

internal class AudioPlayerControllerImpl(
    private val app: Application,
    private val viewModelScope: CoroutineScope,
    dispatchers: CoroutineDispatchers,
    private val LOG: SphinxLogger,
) : AudioPlayerController,
    AudioManager.OnAudioFocusChangeListener,
    CoroutineDispatchers by dispatchers
{

    companion object {
        private const val TAG = "AudioPlayerControllerImpl"
    }

    private inner class AudioStateCache {
        private val lock = Mutex()
        private val map = mutableMapOf<MessageId, MutableStateFlow<AudioMessageState>>()
        private val metaDataRetriever = MediaMetadataRetriever()

        fun releaseMetaDataRetriever() {
            metaDataRetriever.release()
        }

        suspend fun getOrCreate(
            messageId: MessageId,
            file: File?,
            url: String?,
            currentMillis: Long = 0L
        ): MutableStateFlow<AudioMessageState>? {
            var response: MutableStateFlow<AudioMessageState>? = null

            if (file == null && url == null) {
                return null
            }

            viewModelScope.launch(mainImmediate) {

                lock.withLock {

                    val audioPath = file?.path ?: url ?: ""

                    val isLocalFile = file != null

                    map[messageId]?.let { state -> response = state } ?: run {

                        // create new stateful object
                        val durationMillis: Long? = try {
                            delay(50L)

                            withContext(io) {
                                if (Build.VERSION.SDK_INT >= 14 && !isLocalFile) {
                                    metaDataRetriever.setDataSource(audioPath, HashMap<String, String>())
                                } else {
                                    metaDataRetriever.setDataSource(audioPath)
                                }

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
                                    messageId,
                                    file,
                                    url,
                                    AudioPlayState.Paused,
                                    durationMillis,
                                    currentMillis
                                )
                            )

                            map[messageId] = state
                            response = state
                        }
                    }
                }
            }.join()

            return response
        }
    }

    private inner class MediaPlayerHolder {
        private var currentAudio: Pair<MessageId, MutableStateFlow<AudioMessageState>>? = null

        private val mediaPlayer = MediaPlayer().also {
            it.setOnCompletionListener {
                currentAudio?.let { nnCurrent ->
                    dispatchStateJob?.cancel()
                    nnCurrent.second.value = AudioMessageState(
                        nnCurrent.second.value.messageId,
                        nnCurrent.second.value.file,
                        nnCurrent.second.value.url,
                        AudioPlayState.Paused,
                        nnCurrent.second.value.durationMillis,
                        0L,
                    )
                }
            }
            it.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
        private val publicMethodLock = Mutex()

        suspend fun updateMediaState(state: MutableStateFlow<AudioMessageState>) {
            val audioPath = state.value.file?.path ?: state.value.url

            audioPath?.let { nnAudioPath ->
                // only handle valid state objects (file is not null)

                publicMethodLock.withLock {
                    currentAudio?.let { nnCurrent ->
                        if (nnCurrent.first == state.value.messageId) {

                            when (nnCurrent.second.value.playState) {
                                AudioPlayState.Error,
                                AudioPlayState.Loading -> { /* no-op */ }
                                AudioPlayState.Paused -> {
                                    if (requestAudioFocus()) {
                                        playCurrent()
                                    } else {
                                        pauseCurrent()
                                    }
                                }
                                AudioPlayState.Playing -> {
                                    pauseCurrent()
                                }
                            }

                        } else {
                            pauseCurrent()

                            currentAudio = Pair(state.value.messageId, state)

                            state.value = AudioMessageState(
                                state.value.messageId,
                                state.value.file,
                                state.value.url,
                                AudioPlayState.Loading,
                                state.value.durationMillis,
                                state.value.currentMillis,
                            )

                            mediaPlayer.apply {
                                reset()
                                try {
                                    if (setMediaPlayerDataSource(state.value)) {
                                        setOnPreparedListener { mp ->
                                            mp.setOnPreparedListener(null)
                                            mp.seekTo(state.value.currentMillis.toInt())

                                            viewModelScope.launch(mainImmediate) {
                                                if (requestAudioFocus()) {
                                                    playCurrent()
                                                } else {
                                                    pauseCurrent()
                                                }
                                            }
                                        }

                                        prepareAsync()
                                    }
                                } catch (e: IllegalStateException) {

                                    state.value = AudioMessageState(
                                        state.value.messageId,
                                        state.value.file,
                                        state.value.url,
                                        AudioPlayState.Error,
                                        state.value.durationMillis,
                                        state.value.currentMillis,
                                    )

                                }
                            }
                        }
                    } ?: run {
                        currentAudio = Pair(state.value.messageId, state)

                        state.value = AudioMessageState(
                            state.value.messageId,
                            state.value.file,
                            state.value.url,
                            AudioPlayState.Loading,
                            state.value.durationMillis,
                            state.value.currentMillis,
                        )

                        mediaPlayer.apply {
                            reset()
                            try {
                                if (setMediaPlayerDataSource(state.value)) {
                                    setOnPreparedListener { mp ->
                                        mp.setOnPreparedListener(null)
                                        mp.seekTo(state.value.currentMillis.toInt())

                                        viewModelScope.launch(mainImmediate) {
                                            if (requestAudioFocus()) {
                                                playCurrent()
                                            } else {
                                                pauseCurrent()
                                            }
                                        }
                                    }

                                    prepareAsync()
                                }
                            } catch (e: IllegalStateException) {

                                state.value = AudioMessageState(
                                    state.value.messageId,
                                    state.value.file,
                                    state.value.url,
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

        private fun setMediaPlayerDataSource(state: AudioMessageState): Boolean {
            mediaPlayer.apply {
                state.file?.let { nnFile ->
                    setDataSource(app.applicationContext, nnFile.toUri())
                    return true
                }
                state.url?.let { nnUrl ->
                    setDataSource(nnUrl)
                    return true
                }
            }
            return false
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
                            nnCurrent.second.value.messageId,
                            nnCurrent.second.value.file,
                            nnCurrent.second.value.url,
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
                            nnCurrent.second.value.messageId,
                            nnCurrent.second.value.file,
                            nnCurrent.second.value.url,
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
                        delay(250L)
                    }
                }
            }
        }
    }

    private val audioStateCache = AudioStateCache()
    private val mediaPlayerHolder = MediaPlayerHolder()

    //////////////////
    /// Controller ///
    //////////////////

    override suspend fun getAudioState(
        audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable
    ): StateFlow<AudioMessageState>? {
        return audioStateCache.getOrCreate(
            audioAttachment.messageId,
            audioAttachment.file,
            null
        )?.asStateFlow()
    }

    override suspend fun getAudioState(
        podcastClip: LayoutState.Bubble.ContainerSecond.PodcastClip
    ): StateFlow<AudioMessageState>? {
        return audioStateCache.getOrCreate(
            podcastClip.messageId,
            null,
            podcastClip.url,
            (podcastClip.ts * 1000).toLong()
        )?.asStateFlow()
    }

    override fun togglePlayPause(audioAttachment: LayoutState.Bubble.ContainerSecond.AudioAttachment?) {
        if (audioAttachment == null) {
            return
        }

        if (audioAttachment !is LayoutState.Bubble.ContainerSecond.AudioAttachment.FileAvailable) {
            return
        }

        togglePlayPause(audioAttachment.messageId, audioAttachment.file, null)
    }

    override fun togglePlayPause(podcastClip: LayoutState.Bubble.ContainerSecond.PodcastClip?) {
        if (podcastClip == null) {
            return
        }

        if (podcastClip !is LayoutState.Bubble.ContainerSecond.PodcastClip) {
            return
        }

        togglePlayPause(podcastClip.messageId, null, podcastClip.url)
    }

    private var toggleStateJob: Job? = null
    private fun togglePlayPause(
        messageId: MessageId,
        file: File?,
        url: String?
    ) {
        if (toggleStateJob?.isActive == true) {
            return
        }

        toggleStateJob = viewModelScope.launch(mainImmediate) {
            val state = audioStateCache.getOrCreate(messageId, file, url) ?: return@launch
            if (state.value.playState !is AudioPlayState.Loading && state.value.playState !is AudioPlayState.Error) {
                mediaPlayerHolder.updateMediaState(state)
            }
        }
    }

    /**
     * For when user navigates away from screen or sends application to background
     * */
    override fun pauseMediaIfPlaying() {
        viewModelScope.launch(mainImmediate) {
            mediaPlayerHolder.pauseIfPlaying()
        }
    }

    ///////////////////////////
    /// AudioFocus Listener ///
    ///////////////////////////

    private inline val audioManager: AudioManager?
        get() = app
            .applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val attributes: AudioAttributesCompat by lazy {
        AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .build()
    }

    private val request: AudioFocusRequestCompat by lazy {
        AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(this)
            .setWillPauseWhenDucked(true)
            .build()
    }

    private val telephonyManager: TelephonyManager?
        get() = app
            .applicationContext
            .getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private fun requestAudioFocus(): Boolean {
        audioManager?.let { manager ->
            return when (AudioManagerCompat.requestAudioFocus(manager, request)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    false
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    true
                }
                else -> {
                    false
                }
            }
        }

        return false
    }

    override fun onAudioFocusChange(focusChange: Int) {

        val callState = telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE

        when {
            focusChange == AudioManager.AUDIOFOCUS_LOSS                 ||
            callState != TelephonyManager.CALL_STATE_IDLE                   -> {
                pauseMediaIfPlaying()
            }
            focusChange == AudioManager.AUDIOFOCUS_GAIN                     -> {
                // no-op
            }
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT           -> {
                pauseMediaIfPlaying()
            }
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK  -> {
                pauseMediaIfPlaying()
            }
        }
    }

    fun onCleared() {
        audioManager?.let { manager -> AudioManagerCompat.abandonAudioFocusRequest(manager, request) }
        audioStateCache.releaseMetaDataRetriever()
        mediaPlayerHolder.releaseMediaPlayer()
    }
}
