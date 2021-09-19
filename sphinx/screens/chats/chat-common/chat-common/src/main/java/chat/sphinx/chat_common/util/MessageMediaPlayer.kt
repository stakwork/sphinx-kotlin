package chat.sphinx.chat_common.util

import android.media.MediaPlayer
import android.os.CountDownTimer

class MessageMediaPlayer: MediaPlayer() {
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

    override fun reset() {
        super.reset()
        onPlayProgressInfoUpdateListener = null
        cancelCountDownTimer()
    }

    override fun pause() {
        super.pause()
        onPlayProgressInfoUpdateListener?.onPause()
        cancelCountDownTimer()
    }

    override fun start() {
        super.start()
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
}