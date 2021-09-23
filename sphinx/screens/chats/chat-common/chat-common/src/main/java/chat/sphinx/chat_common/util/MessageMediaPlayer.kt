package chat.sphinx.chat_common.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.telephony.TelephonyManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat

class MessageMediaPlayer(
    val context: Context
): MediaPlayer(), AudioManager.OnAudioFocusChangeListener {
    var filePath: String? = null
    var onPlayProgressInfoUpdateListener: OnPlayProgressInfoUpdateListener? = null
    private var countDownTimer: CountDownTimer? = null

    fun load(filePath: String) {
        this.filePath = filePath
        setDataSource(
            filePath
        )
        prepare()
    }

    private fun clear() {
        onPlayProgressInfoUpdateListener = null
        cancelCountDownTimer()
        abandonAudioFocus()
    }

    override fun reset() {
        super.reset()
        clear()
    }

    override fun release() {
        super.release()
        clear()
    }

    override fun pause() {
        super.pause()
        onPlayProgressInfoUpdateListener?.onPause()
        cancelCountDownTimer()
    }

    override fun start() {
        if (requestAudioFocus()) {
            super.start()
        }
        onPlayProgressInfoUpdateListener?.onPlay()
    }

    fun initPlayProgressInfoUpdateWithTimer(progress: Int) {
        cancelCountDownTimer()

        val remainingTime = duration - progress
        countDownTimer = object: CountDownTimer(remainingTime.toLong(), 100) {
            override fun onTick(millisUntilFinished: Long) {
                onPlayProgressInfoUpdateListener?.onPlayProgressUpdate(millisUntilFinished)
            }

            override fun onFinish() {
                onPlayProgressInfoUpdateListener?.onFinish()
            }
        }
        countDownTimer?.start()
    }

    private fun cancelCountDownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    interface OnPlayProgressInfoUpdateListener  {
        fun onPlayProgressUpdate(millisUntilFinished: Long)

        fun onFinish()

        fun onPause()

        fun onPlay()
    }

    private inline val audioManager: AudioManager?
        get() = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val attributes: AudioAttributesCompat by lazy {
        AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .build()
    }

    private val request: AudioFocusRequestCompat by lazy {
        AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(this)
            .setWillPauseWhenDucked(true)
            .build()
    }

    @JvmSynthetic
    fun requestAudioFocus(): Boolean {
        audioManager?.let { manager ->
            return when (AudioManagerCompat.requestAudioFocus(manager, request)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    false
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
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
    fun abandonAudioFocus(): Boolean {
        audioManager?.let { manager ->
            return when (AudioManagerCompat.abandonAudioFocusRequest(manager, request)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    false
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    true
                }
                else -> {
                    false
                }
            }
        }
        return false
    }

    private inline val telephonyManager: TelephonyManager?
        get() = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    override fun onAudioFocusChange(focusChange: Int) {

        val callState = telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE

        if (callState != TelephonyManager.CALL_STATE_IDLE) {
            pause()
        } else {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (isPlaying) {
                        pause()
                    }
                }
            }
        }
    }
}