package chat.sphinx.feature_service_media_player_android.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl

internal abstract class MediaPlayerService: Service() {

    protected abstract val mediaServiceController: MediaPlayerServiceControllerImpl

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
