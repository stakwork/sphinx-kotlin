package chat.sphinx.feature_service_media_player_android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import chat.sphinx.feature_service_media_player_android.model.PodcastDataHolder
import chat.sphinx.feature_service_media_player_android.service.components.AudioManagerHandler
import chat.sphinx.feature_service_media_player_android.service.components.MediaPlayerNotification
import chat.sphinx.feature_service_media_player_android.util.toServiceActionPlay
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_foreground_state.ForegroundState
import io.matthewnelson.concept_foreground_state.ForegroundStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

internal abstract class MediaPlayerService: Service() {

    companion object {
        const val TAG = "MediaPlayerService"
    }

    abstract val serviceContext: Context

    protected abstract val dispatchers: CoroutineDispatchers
    protected abstract val audioManagerHandler: AudioManagerHandler
    protected abstract val foregroundStateManager: ForegroundStateManager
    protected abstract val LOG: SphinxLogger
    abstract val mediaServiceController: MediaPlayerServiceControllerImpl
    protected abstract val repositoryMedia: RepositoryMedia

    private val supervisor = SupervisorJob()
    protected val serviceLifecycleScope = CoroutineScope(supervisor)

    @Suppress("DEPRECATION")
    private val wifiLock: WifiManager.WifiLock? by lazy {
        (getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.createWifiLock(WifiManager.WIFI_MODE_FULL, this.javaClass.simpleName)
    }

    private val notification: MediaPlayerNotification by lazy {
        MediaPlayerNotification(this)
    }

    @Volatile
    protected var currentState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceActive.ServiceLoading
        private set

    inner class MediaPlayerHolder {
        @Volatile
        private var podData: PodcastDataHolder? = null

        private val audioAttributes: AudioAttributes by lazy {
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        }

        @Synchronized
        fun audioFocusLost() {
            podData?.let { nnData ->
                if (nnData.mediaPlayer.isPlaying) {
                    pausePlayer(
                        chatId = nnData.chatId,
                        episodeId = nnData.episodeId,
                        abandonAudioFocus = false
                    )
                }
            }
        }

        @Synchronized
        fun audioFocusGained() {
            podData?.let { nnData ->
                if (!nnData.mediaPlayer.isPlaying) {
                    try {
                        nnData.mediaPlayer.start()
                    } catch (e: IllegalStateException) {
                        LOG.e(TAG, "AudioFocusGained failed to start MediaPlayer", e)
                        // TODO: Handle Exception
                    }

                    if (nnData.mediaPlayer.isPlaying) {
                        if (stateDispatcherJob?.isActive != true) {
                            startStateDispatcher()
                        }
                        wifiLock?.let { lock ->
                            if (!lock.isHeld) {
                                lock.acquire()
                            }
                        }
                    } else {
                        wifiLock?.let { lock ->
                            if (lock.isHeld) {
                                lock.release()
                            }
                        }
                    }
                }
            }
        }

        @Synchronized
        fun processUserAction(userAction: UserAction) {
            @Exhaustive
            when (userAction) {
                is UserAction.AdjustSpeed -> {

                    podData?.let { nnData ->
                        if (
                            nnData.chatId == userAction.chatId &&
                            nnData.episodeId == userAction.chatMetaData.itemId.value
                        ) {
                            try {
                                val playing = nnData.mediaPlayer.isPlaying
                                nnData.setSpeed(userAction.chatMetaData.speed).also {
                                    nnData.mediaPlayer.playbackParams =
                                        nnData.mediaPlayer.playbackParams.setSpeed(it.toFloat())
                                }
                                if (!playing) {
                                    nnData.mediaPlayer.pause()
                                }
                            } catch (e: IllegalStateException) {
                                LOG.e(TAG, "Failed to adjust speed for MediaPlayer", e)
                                // TODO: Handle Error
                            }
                        }
                    }

                    repositoryMedia.updateChatMetaData(userAction.chatId, userAction.chatMetaData)

                }
                is UserAction.SetPaymentsDestinations -> {
                    podData?.let { nnData ->
                        nnData.setDestinations(userAction.destinations)
                    }
                }
                is UserAction.ServiceAction.Pause -> {

                    pausePlayer(
                        chatId = userAction.chatId,
                        episodeId = userAction.episodeId,
                        abandonAudioFocus = true
                    )

                }
                is UserAction.ServiceAction.Play -> {

                    podData?.let { nnData ->
                        if (
                            nnData.chatId == userAction.chatId &&
                            nnData.episodeId == userAction.episodeId
                        ) {

                            if (!nnData.mediaPlayer.isPlaying) {
                                try {
                                    nnData.mediaPlayer.seekTo(userAction.startTime)
                                    nnData.setSpeed(userAction.speed)

                                    if (audioManagerHandler.requestAudioFocus()) {
                                        nnData.mediaPlayer.playbackParams =
                                            nnData.mediaPlayer.playbackParams.setSpeed(userAction.speed.toFloat())
                                        nnData.mediaPlayer.start()
                                    }
                                } catch (e: IllegalStateException) {
                                    LOG.e(TAG, "Failed to start MediaPlayer", e)
                                    // TODO: Handle Error
                                }
                            }

                            if (nnData.mediaPlayer.isPlaying) {
                                if (stateDispatcherJob?.isActive != true) {
                                    startStateDispatcher()
                                }
                                wifiLock?.let { lock ->
                                    if (!lock.isHeld) {
                                        lock.acquire()
                                    }
                                }
                            } else {
                                wifiLock?.let { lock ->
                                    if (lock.isHeld) {
                                        lock.release()
                                    }
                                }
                            }


                        } else {

                            stateDispatcherJob?.cancel()

                            currentState = MediaPlayerServiceState.ServiceActive.ServiceLoading
                            mediaServiceController.dispatchState(currentState)

                            currentState = MediaPlayerServiceState.ServiceActive.ServiceLoaded
                            mediaServiceController.dispatchState(currentState)

                            repositoryMedia.updateChatMetaData(
                                nnData.chatId,
                                ChatMetaData(
                                    ItemId(nnData.episodeId),
                                    nnData.satsPerMinute,
                                    nnData.mediaPlayer.currentPosition,
                                    nnData.speed
                                )
                            )

                            createMediaPlayer(userAction, nnData.mediaPlayer)

                        }
                    } ?: createMediaPlayer(userAction, null)

                    repositoryMedia.updateChatMetaData(
                        userAction.chatId,
                        ChatMetaData(
                            ItemId(userAction.episodeId),
                            userAction.satPerMinute,
                            userAction.startTime,
                            userAction.speed
                        )
                    )

                }
                is UserAction.ServiceAction.Seek -> {
                    podData?.let { nnPlayer ->
                        if (
                            nnPlayer.chatId == userAction.chatId &&
                            nnPlayer.episodeId == userAction.chatMetaData.itemId.value
                        ) {
                            try {
                                nnPlayer.mediaPlayer.seekTo(userAction.chatMetaData.timeSeconds)
                                // TODO: Dispatch State
                            } catch (e: IllegalStateException) {
                                LOG.e(
                                    TAG,
                                    "Failed to seekTo ${userAction.chatMetaData.timeSeconds} for MediaPlayer",
                                    e
                                )
                                // TODO: Handle Error
                            }
                        }
                    }

                    repositoryMedia.updateChatMetaData(userAction.chatId, userAction.chatMetaData)
                }
            }
        }

        private fun pausePlayer(
            chatId: ChatId,
            episodeId: Long,
            abandonAudioFocus: Boolean = false
        ) {
            podData?.let { nnData ->
                if (
                    nnData.chatId == chatId &&
                    nnData.episodeId == episodeId
                ) {
                    try {
                        nnData.mediaPlayer.pause()

                        wifiLock?.let { lock ->
                            if (lock.isHeld) {
                                lock.release()
                            }
                        }

                        if (abandonAudioFocus) {
                            audioManagerHandler.abandonAudioFocus()
                        }

                        stateDispatcherJob?.cancel()
                        val currentTime = nnData.mediaPlayer.currentPosition

                        currentState = MediaPlayerServiceState.ServiceActive.MediaState.Paused(
                            nnData.chatId,
                            nnData.episodeId,
                            currentTime
                        )
                        mediaServiceController.dispatchState(currentState)

                        repositoryMedia.updateChatMetaData(
                            chatId,
                            ChatMetaData(
                                ItemId(nnData.episodeId),
                                nnData.satsPerMinute,
                                currentTime,
                                nnData.speed,
                            )
                        )

                    } catch (e: IllegalStateException) {
                        LOG.e(TAG, "Failed to pause MediaPlayer", e)
                        // TODO: Handle Error
                    }
                }
            }
        }

        private fun createMediaPlayer(
            userAction: UserAction.ServiceAction.Play,
            mediaPlayer: MediaPlayer?,
        ) {
            val player: MediaPlayer = mediaPlayer.also { it?.reset() } ?: MediaPlayer()

            player.apply {
                setAudioAttributes(audioAttributes)
                setWakeMode(serviceContext.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(userAction.episodeUrl)
                setOnPreparedListener { mp ->
                    mp.setOnPreparedListener(null)
                    mp.seekTo(userAction.startTime)
                    mp.playbackParams = mp.playbackParams.setSpeed(userAction.speed.toFloat())

                    if (audioManagerHandler.requestAudioFocus()) {
                        mp.start()
                    } else {
                        mp.pause()
                    }

                    startStateDispatcher()
                }
            }.let { mp ->
                wifiLock?.let { lock ->
                    if (!lock.isHeld) {
                        lock.acquire()
                    }
                }

                mp.prepareAsync()
                podData = PodcastDataHolder.instantiate(
                    userAction.chatId,
                    userAction.podcastId,
                    userAction.episodeId,
                    userAction.satPerMinute,
                    mp,
                    userAction.speed
                )
            }
        }

        private var stateDispatcherJob: Job? = null
        private fun startStateDispatcher() {
            stateDispatcherJob?.cancel()
            stateDispatcherJob = serviceLifecycleScope.launch(dispatchers.mainImmediate) {
                var count: Double = 0.0
                while (isActive) {
                    val speed: Double = podData?.speed ?: 1.0
                    podData?.let { nnData ->
                        val currentTime = nnData.mediaPlayer.currentPosition

                        if (count >= 60.0 * speed) {
                            Log.d("PODCAST", "THIS IS A ONE MINUTE NOTIFICATION")

                            repositoryMedia.streamPodcastPayments(
                                nnData.chatId,
                                ChatMetaData(
                                    ItemId(nnData.episodeId),
                                    nnData.satsPerMinute,
                                    currentTime,
                                    speed,
                                ),
                                nnData.podcastId,
                                nnData.episodeId,
                                nnData.destinations
                            )
                            count = 0.0
                        } else {
                            count += 1.0
                        }

                        if (nnData.mediaPlayer.isPlaying) {
                            currentState = MediaPlayerServiceState.ServiceActive.MediaState.Playing(
                                nnData.chatId,
                                nnData.episodeId,
                                currentTime
                            )
                        } else {

                            val state = if (nnData.mediaPlayer.duration <= currentTime) {
                                MediaPlayerServiceState.ServiceActive.MediaState.Ended(
                                    nnData.chatId,
                                    nnData.episodeId,
                                    currentTime
                                )
                            } else {
                                MediaPlayerServiceState.ServiceActive.MediaState.Paused(
                                    nnData.chatId,
                                    nnData.episodeId,
                                    currentTime
                                )
                            }

                            currentState = state
                            mediaServiceController.dispatchState(state)

                            if (state is MediaPlayerServiceState.ServiceActive.MediaState.Ended) {
                                shutDownService()
                            }

                            stateDispatcherJob?.cancelAndJoin()
                        }
                    }
                    mediaServiceController.dispatchState(currentState)

                    delay(1_000_000 / (speed * 1000).toLong())
                }
            }
        }

        @Synchronized
        fun clear() {
            stateDispatcherJob?.cancel()
            currentState = MediaPlayerServiceState.ServiceInactive
            mediaServiceController.dispatchState(currentState)
            notification.clear()
            audioManagerHandler.abandonAudioFocus()
            podData?.let { data ->
                repositoryMedia.updateChatMetaData(
                    data.chatId,
                    ChatMetaData(
                        ItemId(data.episodeId),
                        data.satsPerMinute,
                        data.mediaPlayer.currentPosition,
                        data.speed,
                    )
                )
                data.mediaPlayer.release()
                podData = null
            }
        }
    }

    val mediaPlayerHolder: MediaPlayerHolder by lazy {
        MediaPlayerHolder()
    }

    inner class MediaPlayerServiceBinder: Binder() {
        fun getCurrentState(): MediaPlayerServiceState {
            return currentState
        }

        fun processUserAction(userAction: UserAction) {
            mediaPlayerHolder.processUserAction(userAction)
        }
    }

    private val binder: MediaPlayerServiceBinder by lazy(LazyThreadSafetyMode.NONE) {
        MediaPlayerServiceBinder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private var rebindJob: Job? = null
    override fun onCreate() {
        super.onCreate()
        mediaServiceController.dispatchState(currentState)
        notification
        rebindJob = serviceLifecycleScope.launch(dispatchers.mainImmediate) {
            foregroundStateManager.foregroundStateFlow.collect { foregroundState ->
                @Exhaustive
                when (foregroundState) {
                    ForegroundState.Background -> {
                        // AndroidOS automatically unbinds service when
                        // application is sent to the background, so we
                        // rebind it here to ensure we maintain a started,
                        // bound service.
                        mediaServiceController.bindService()
                    }
                    ForegroundState.Foreground -> {}
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.toServiceActionPlay()?.let { mediaPlayerHolder.processUserAction(it) }
        // TODO: If Null, stop service and don't post notification

        return START_NOT_STICKY
//        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        shutDownService()
    }

    @JvmSynthetic
    fun shutDownService() {
        rebindJob?.cancel()
        wifiLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        mediaServiceController.unbindService()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerHolder.clear()
        supervisor.cancel()
    }
}
