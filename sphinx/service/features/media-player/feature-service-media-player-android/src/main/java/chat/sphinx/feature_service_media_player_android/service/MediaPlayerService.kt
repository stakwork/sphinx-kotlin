package chat.sphinx.feature_service_media_player_android.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import chat.sphinx.feature_service_media_player_android.util.toServiceActionPlay
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_foreground_state.ForegroundState
import io.matthewnelson.concept_foreground_state.ForegroundStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal abstract class MediaPlayerService: Service() {

    protected abstract val dispatchers: CoroutineDispatchers
    protected abstract val foregroundStateManager: ForegroundStateManager
    protected abstract val mediaServiceController: MediaPlayerServiceControllerImpl
    protected abstract val repositoryMedia: RepositoryMedia

    @Volatile
    protected var currentState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceActive.ServiceLoading
        private set

    inner class MediaPlayerServiceBinder: Binder() {
        fun getCurrentState(): MediaPlayerServiceState {
            // TODO: Implement
            return currentState
        }

        fun processUserAction(userAction: UserAction) {
            this@MediaPlayerService.processUserAction(userAction)
        }
    }

    private val binder: MediaPlayerServiceBinder by lazy {
        MediaPlayerServiceBinder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    protected var mediaPlayer: MediaPlayer? = null
        private set

    private fun processUserAction(userAction: UserAction) {
        @Exhaustive
        when (userAction) {
            is UserAction.AdjustSpeed -> {

            }
            is UserAction.ServiceAction.Pause -> {

            }
            is UserAction.ServiceAction.Play -> {

            }
            is UserAction.ServiceAction.Seek -> {

            }
        }
    }

    private val supervisor = SupervisorJob()
    protected val serviceLifecycleScope = CoroutineScope(supervisor)

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
        intent?.toServiceActionPlay()?.let { processUserAction(it) }
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
        mediaPlayer?.release()
        mediaServiceController.dispatchState(currentState)
        // TODO: Clear notification
        supervisor.cancel()
    }
}
