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
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import chat.sphinx.feature_service_media_player_android.model.PodcastDataHolder
import chat.sphinx.feature_service_media_player_android.util.toServiceActionPlay
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
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
    protected abstract val foregroundStateManager: ForegroundStateManager
    protected abstract val LOG: SphinxLogger
    abstract val mediaServiceController: MediaPlayerServiceControllerImpl
    protected abstract val repositoryMedia: RepositoryMedia

    private val supervisor = SupervisorJob()
    protected val serviceLifecycleScope = CoroutineScope(supervisor)

    @Volatile
    protected var currentState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceActive.ServiceLoading
        private set

    inner class MediaPlayerHolder {
        @Volatile
        private var podData: PodcastDataHolder? = null

        @Suppress("DEPRECATION")
        private val wifiLock: WifiManager.WifiLock? by lazy {
            (getSystemService(Context.WIFI_SERVICE) as? WifiManager)
                ?.createWifiLock(WifiManager.WIFI_MODE_FULL, this.javaClass.simpleName)
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
                                nnData.mediaPlayer.playbackParams.apply {
                                    speed = userAction.chatMetaData.speed.toFloat()
                                }
                            } catch (e: IllegalStateException) {
                                LOG.e(TAG, "Failed to adjust speed for MediaPlayer", e)
                                // TODO: Handle Error
                            }
                        }
                    }

                    repositoryMedia.updateChatMetaData(userAction.chatId, userAction.chatMetaData)

                }
                is UserAction.ServiceAction.Pause -> {

                    podData?.let { nnData ->
                        if (
                            nnData.chatId == userAction.chatId &&
                            nnData.episodeId == userAction.episodeId
                        ) {
                            try {
                                nnData.mediaPlayer.pause()

                                stateDispatcherJob?.cancel()
                                val currentTime = nnData.mediaPlayer.currentPosition

                                currentState = MediaPlayerServiceState.ServiceActive.MediaState.Paused(
                                    nnData.chatId,
                                    nnData.episodeId,
                                    currentTime
                                )
                                mediaServiceController.dispatchState(currentState)

                                repositoryMedia.updateChatMetaData(
                                    userAction.chatId,
                                    ChatMetaData(
                                        ItemId(nnData.episodeId),
                                        nnData.satsPerMinute,
                                        currentTime,
                                        nnData.mediaPlayer.playbackParams.speed.toDouble(),
                                    )
                                )

                            } catch (e: IllegalStateException) {
                                LOG.e(TAG, "Failed to pause MediaPlayer", e)
                                // TODO: Handle Error
                            }
                        }
                    }

                    wifiLock?.release()

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
                                    nnData.mediaPlayer.start()
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
                                wifiLock?.release()
                            }


                        } else {

                            val currentTime = nnData.mediaPlayer.currentPosition
                            val speed = nnData.mediaPlayer.playbackParams.speed.toDouble()

                            stateDispatcherJob?.cancel()
                            nnData.mediaPlayer.stop()
                            currentState = MediaPlayerServiceState.ServiceActive.MediaState.Paused(
                                nnData.chatId,
                                nnData.episodeId,
                                currentTime
                            )
                            mediaServiceController.dispatchState(currentState)

                            repositoryMedia.updateChatMetaData(
                                nnData.chatId,
                                ChatMetaData(
                                    ItemId(nnData.episodeId),
                                    nnData.satsPerMinute,
                                    currentTime,
                                    speed
                                )
                            )

                            nnData.mediaPlayer.setDataSource(userAction.episodeUrl)
                            nnData.mediaPlayer.setOnPreparedListener { mp ->
                                mp.setOnPreparedListener(null)
                                mp.seekTo(userAction.startTime)
                                mp.start()
                                startStateDispatcher()
                            }
                            nnData.mediaPlayer.prepareAsync()
                            podData = PodcastDataHolder(
                                userAction.chatId,
                                userAction.episodeId,
                                userAction.satPerMinute,
                                nnData.mediaPlayer,
                            )

                            wifiLock?.let { lock ->
                                if (!lock.isHeld) {
                                    lock.acquire()
                                }
                            }

                        }
                    } ?: MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setWakeMode(serviceContext.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                        setDataSource(userAction.episodeUrl)
                        setOnPreparedListener { mp ->
                            mp.setOnPreparedListener(null)
                            mp.seekTo(userAction.startTime)
                            mp.start()
                            startStateDispatcher()
                        }
                    }.let { mp ->
                        wifiLock?.let { lock ->
                            if (!lock.isHeld) {
                                lock.acquire()
                            }
                        }

                        mp.prepareAsync()
                        podData = PodcastDataHolder(
                            userAction.chatId,
                            userAction.episodeId,
                            userAction.satPerMinute,
                            mp,
                        )
                    }

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

        private var stateDispatcherJob: Job? = null
        private fun startStateDispatcher() {
            stateDispatcherJob?.cancel()
            stateDispatcherJob = serviceLifecycleScope.launch(dispatchers.mainImmediate) {
                var count = 0
                while (isActive) {
                    podData?.let { nnData ->

                        val currentTime = nnData.mediaPlayer.currentPosition

                        if (count >= 60) {
                            repositoryMedia.updateChatMetaData(
                                nnData.chatId,
                                ChatMetaData(
                                    ItemId(nnData.episodeId),
                                    nnData.satsPerMinute,
                                    currentTime,
                                    nnData.mediaPlayer.playbackParams.speed.toDouble()
                                )
                            )
                            count = 0
                        } else {
                            count++
                        }

                        if (nnData.mediaPlayer.isPlaying) {
                            currentState = MediaPlayerServiceState.ServiceActive.MediaState.Playing(
                                nnData.chatId,
                                nnData.episodeId,
                                currentTime
                            )
                        } else {

                            currentState = if (nnData.mediaPlayer.duration == currentTime) {
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

                            mediaServiceController.dispatchState(currentState)
                            stateDispatcherJob?.cancelAndJoin()

                        }
                    }
                    mediaServiceController.dispatchState(currentState)
                    delay(1_000)
                }
            }
        }

        @Synchronized
        fun clear() {
            stateDispatcherJob?.cancel()
            wifiLock?.release()
            currentState = MediaPlayerServiceState.ServiceInactive
            mediaServiceController.dispatchState(currentState)
            podData?.let { data ->
                repositoryMedia.updateChatMetaData(
                    data.chatId,
                    ChatMetaData(
                        ItemId(data.episodeId),
                        data.satsPerMinute,
                        data.mediaPlayer.currentPosition,
                        data.mediaPlayer.playbackParams.speed.toDouble(),
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

    private val notification: MediaPlayerNotification by lazy {
        MediaPlayerNotification(this)
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
        mediaServiceController.unbindService()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerHolder.clear()
        // TODO: Clear notification
        supervisor.cancel()
    }
}
