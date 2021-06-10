package chat.sphinx.feature_service_media_player_android.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl

internal abstract class MediaPlayerService: Service() {

    protected abstract val mediaServiceController: MediaPlayerServiceControllerImpl

    private var mediaPlayer: MediaPlayer? = null

    inner class MediaPlayerServiceBinder: Binder() {
        fun getCurrentState(): MediaPlayerServiceState.ServiceActive {
            // TODO: Implement
            return MediaPlayerServiceState.ServiceActive.ServiceLoading
        }
    }

    private val binder: MediaPlayerServiceBinder by lazy {
        MediaPlayerServiceBinder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
}
