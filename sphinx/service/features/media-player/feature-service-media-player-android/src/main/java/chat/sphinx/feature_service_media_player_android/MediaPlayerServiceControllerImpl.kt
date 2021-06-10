package chat.sphinx.feature_service_media_player_android

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.service.MediaPlayerService
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MediaPlayerServiceControllerImpl(
    dispatchers: CoroutineDispatchers
): MediaPlayerServiceController(), CoroutineDispatchers by dispatchers {

    private val binder: MutableStateFlow<MediaPlayerService.MediaPlayerServiceBinder?> by lazy {
        MutableStateFlow(null)
    }

    override fun getCurrentState(): MediaPlayerServiceState {
        return binder.value?.getCurrentState() ?: if (userActionLock.isLocked) {
            // b/c this is only called when adding a new listener
            // and is synchronized, if the lock is currently
            // held by a user action being processed that will
            // not result in starting the service, thereby dispatching
            // an updated state, that user action processing will
            // come _after_ this is called and will be set properly
            // after the user action processing clears.
            MediaPlayerServiceState.ServiceActive.ServiceLoading
        } else {
            MediaPlayerServiceState.ServiceInactive
        }
    }

    @JvmSynthetic
    fun clearBinderReference() {
        binder.value = null
    }

    @JvmSynthetic
    fun dispatchState(mediaPlayerServiceState: MediaPlayerServiceState) {
        listenerHandler.dispatch(mediaPlayerServiceState)
    }

    inner class MediaPlayerServiceConnection: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null) {
                binder.value = service as MediaPlayerService.MediaPlayerServiceBinder
            } else {
                clearBinderReference()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            clearBinderReference()
        }
    }

    private val connection: MediaPlayerServiceConnection by lazy {
        MediaPlayerServiceConnection()
    }

    private val userActionLock = Mutex()
    override suspend fun processAction(userAction: UserAction) {
        binder.value?.processUserAction(userAction) ?: when (userAction) {
            is UserAction.AdjustSpeed -> {
                // TODO: Update speed for given chatId
            }
            is UserAction.ServiceAction.Pause -> {
                // TODO: dispatch state to update the UI
            }
            is UserAction.ServiceAction.Play -> {
                userActionLock.withLock {
                    binder.value?.processUserAction(userAction) ?: startService(userAction)
                }
            }
            is UserAction.ServiceAction.Seek -> {
                // TODO: update the chat's metadata with the info on the
                //  current episode and new time, then dispatch state as service
                //  is not available
            }
        }
    }

    private suspend fun startService(play: UserAction.ServiceAction.Play) {

    }
}
