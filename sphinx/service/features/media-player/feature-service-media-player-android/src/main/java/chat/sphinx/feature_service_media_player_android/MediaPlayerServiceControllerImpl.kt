package chat.sphinx.feature_service_media_player_android

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.feature_service_media_player_android.service.MediaPlayerService
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow

internal class MediaPlayerServiceControllerImpl(
    dispatchers: CoroutineDispatchers
): MediaPlayerServiceController(), CoroutineDispatchers by dispatchers {

    private val binder: MutableStateFlow<MediaPlayerService.MediaPlayerServiceBinder?> by lazy {
        MutableStateFlow(null)
    }

    override fun getCurrentState(): MediaPlayerServiceState {
        return binder.value?.getCurrentState() ?: MediaPlayerServiceState.ServiceInactive
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
}
