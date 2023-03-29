package chat.sphinx.feature_service_media_player_android.service.components

import android.content.Context
import android.media.AudioManager
import android.telephony.TelephonyManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import chat.sphinx.feature_service_media_player_android.service.MediaPlayerService

internal abstract class AudioManagerHandler(
    protected val mediaPlayerService: MediaPlayerService,
): AudioManager.OnAudioFocusChangeListener {

    protected inline val audioManager: AudioManager?
        get() = mediaPlayerService
            .serviceContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val attributes: AudioAttributesCompat by lazy {
        AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .build()
    }

    protected val request: AudioFocusRequestCompat by lazy {
        AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(this)
            .setWillPauseWhenDucked(true)
            .build()
    }

    @JvmSynthetic
    abstract fun requestAudioFocus(): Boolean

    @JvmSynthetic
    abstract fun abandonAudioFocus(): Boolean

    private inline val telephonyManager: TelephonyManager?
        get() = mediaPlayerService
            .serviceContext
            .getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    override fun onAudioFocusChange(focusChange: Int) {

        val callState = telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE

        when {
            focusChange == AudioManager.AUDIOFOCUS_LOSS                 ||
            callState != TelephonyManager.CALL_STATE_IDLE                   -> {
                mediaPlayerService.mediaPlayerHolder.audioFocusLost()
            }
            focusChange == AudioManager.AUDIOFOCUS_GAIN                     -> {
                mediaPlayerService.mediaPlayerHolder.audioFocusGained()
            }

            // TODO: Build out transient loss
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT           -> {
                mediaPlayerService.mediaPlayerHolder.audioFocusLost()
            }
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK  -> {
                mediaPlayerService.mediaPlayerHolder.audioFocusLost()
            }
        }

    }
}