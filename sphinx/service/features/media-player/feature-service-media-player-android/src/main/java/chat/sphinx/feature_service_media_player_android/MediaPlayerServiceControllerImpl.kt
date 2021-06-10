package chat.sphinx.feature_service_media_player_android

import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class MediaPlayerServiceControllerImpl(
    dispatchers: CoroutineDispatchers
): MediaPlayerServiceController(), CoroutineDispatchers by dispatchers {

    override fun getCurrentState(): MediaPlayerServiceState {
        // TODO: Retrieve MediaPlayer state from binder
        return MediaPlayerServiceState.ServiceInactive
    }

    fun dispatchState(mediaPlayerServiceState: MediaPlayerServiceState) {
        listenerHandler.dispatch(mediaPlayerServiceState)
    }
}
