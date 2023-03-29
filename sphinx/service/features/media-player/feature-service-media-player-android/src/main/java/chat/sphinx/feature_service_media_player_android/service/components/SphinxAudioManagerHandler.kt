package chat.sphinx.feature_service_media_player_android.service.components

import android.media.AudioManager
import android.util.Log
import androidx.media.AudioManagerCompat
import chat.sphinx.feature_service_media_player_android.service.MediaPlayerService

internal class SphinxAudioManagerHandler(
    mediaPlayerService: MediaPlayerService,
): AudioManagerHandler(mediaPlayerService) {

    @JvmSynthetic
    override fun requestAudioFocus(): Boolean {
        audioManager?.let { manager ->
            return when (AudioManagerCompat.requestAudioFocus(manager, request)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    false
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    mediaPlayerService.mediaPlayerHolder.soundIsComingOut()
                    true
                }
                else -> {
                    false
                }
            }
        }

        return false
    }

    @JvmSynthetic
    override fun abandonAudioFocus(): Boolean {
        audioManager?.let { manager ->
            return when (AudioManagerCompat.abandonAudioFocusRequest(manager, request)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    false
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    mediaPlayerService.mediaPlayerHolder.soundIsNotComingOut()
                    true
                }
                else -> {
                    false
                }
            }
        }

        return false
    }
}