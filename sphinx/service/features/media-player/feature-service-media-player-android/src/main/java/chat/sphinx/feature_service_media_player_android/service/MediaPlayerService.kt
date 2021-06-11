package chat.sphinx.feature_service_media_player_android.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import chat.sphinx.feature_service_media_player_android.util.toServiceActionPlay
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_foreground_state.ForegroundState
import io.matthewnelson.concept_foreground_state.ForegroundStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal abstract class MediaPlayerService: Service() {

    companion object {
        const val TAG = "MediaPlayerService"
    }

    protected abstract val dispatchers: CoroutineDispatchers
    protected abstract val foregroundStateManager: ForegroundStateManager
    protected abstract val LOG: SphinxLogger
    protected abstract val mediaServiceController: MediaPlayerServiceControllerImpl
    protected abstract val repositoryMedia: RepositoryMedia

    private val supervisor = SupervisorJob()
    protected val serviceLifecycleScope = CoroutineScope(supervisor)

    @Volatile
    protected var currentState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceActive.ServiceLoading
        private set

    protected inner class MediaPlayerHolder {
        // chatId, episodeId, mediaPlayer
        @Volatile
        private var player: Triple<ChatId, Long, MediaPlayer>? = null

        @Synchronized
        fun processUserAction(userAction: UserAction) {
            @Exhaustive
            when (userAction) {
                is UserAction.AdjustSpeed -> {

                    player?.let { nnPlayer ->
                        if (
                            nnPlayer.first == userAction.chatId &&
                            nnPlayer.second == userAction.chatMetaData.itemId.value
                        ) {
                            try {
                                nnPlayer.third.playbackParams.apply {
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

                        player?.let { nnPlayer ->
                            if (
                                nnPlayer.first == userAction.chatId &&
                                nnPlayer.second == userAction.episodeId
                            ) {
                                try {
                                    nnPlayer.third.pause()

                                    repositoryMedia.updateChatMetaData(
                                        userAction.chatId,
                                        ChatMetaData(
                                            ItemId(nnPlayer.second),
                                            userAction.satPerMinute,
                                            nnPlayer.third.currentPosition,
                                            nnPlayer.third.playbackParams.speed.toDouble(),
                                        )
                                    )

                                    // TODO: Cancel dispatch coroutine
                                    // TODO: Update and dispatch current state
                                } catch (e: IllegalStateException) {
                                    LOG.e(TAG, "Failed to pause MediaPlayer", e)
                                    // TODO: Handle Error
                                }
                            }
                        }

                }
                is UserAction.ServiceAction.Play -> {

                    player?.let { nnPlayer ->
                        if (
                            nnPlayer.first == userAction.chatId &&
                            nnPlayer.second == userAction.episodeId
                        ) {

                            if (!nnPlayer.third.isPlaying) {
                                try {
                                    nnPlayer.third.seekTo(userAction.startTime)
                                    nnPlayer.third.start()
                                } catch (e: IllegalStateException) {
                                    LOG.e(TAG, "Failed to start MediaPlayer", e)
                                    // TODO: Handle Error
                                }
                            }

                        } else {

                            nnPlayer.third.stop()
                            nnPlayer.third.setOnPreparedListener { mp ->
                                mp.setOnPreparedListener(null)
                                mp.seekTo(userAction.startTime)
                                mp.start()
                                // TODO: Start state dispatcher coroutine
                            }
                            nnPlayer.third.prepareAsync()
                            player = Triple(userAction.chatId, userAction.episodeId, nnPlayer.third)

                        }
                    } ?: MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(userAction.episodeUrl)
                        setOnPreparedListener { mp ->
                            mp.setOnPreparedListener(null)
                            mp.seekTo(userAction.startTime)
                            mp.start()
                            // TODO: Start state dispatcher coroutine
                        }
                    }.let { mp ->
                        mp.prepareAsync()
                        player = Triple(userAction.chatId, userAction.episodeId, mp)
                    }

                }
                is UserAction.ServiceAction.Seek -> {
                    player?.let { nnPlayer ->
                        if (
                            nnPlayer.first == userAction.chatId &&
                            nnPlayer.second == userAction.chatMetaData.itemId.value
                        ) {
                            try {
                                nnPlayer.third.seekTo(userAction.chatMetaData.timeSeconds)
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

        @Synchronized
        fun release() {
            player?.third?.release()
            player = null
        }
    }

    protected val mediaPlayerHolder: MediaPlayerHolder by lazy {
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
        rebindJob?.cancel()
        mediaServiceController.unbindService()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentState = MediaPlayerServiceState.ServiceInactive
        mediaPlayerHolder.release()
        mediaServiceController.dispatchState(currentState)
        // TODO: Clear notification
        supervisor.cancel()
    }
}
