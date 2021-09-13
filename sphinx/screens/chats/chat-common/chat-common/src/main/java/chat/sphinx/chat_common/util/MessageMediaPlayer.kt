package chat.sphinx.chat_common.util

import android.media.MediaPlayer
import android.os.CountDownTimer

class MessageMediaPlayer: MediaPlayer() {
    var filePath: String? = null

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

        cancelCountDownTimer()
    }

    override fun pause() {
        super.pause()

        cancelCountDownTimer()
    }

    fun initPlayProgressInfoUpdateWithTimer(progress: Int, onPlayProgressInfoUpdateListener: OnPlayProgressInfoUpdateListener) {
        cancelCountDownTimer()

        val remainingTime = duration - progress
        countDownTimer = object: CountDownTimer(remainingTime.toLong(), 100) {
            override fun onTick(millisUntilFinished: Long) {
                onPlayProgressInfoUpdateListener.onPlayProgressUpdate(millisUntilFinished)
            }

            override fun onFinish() {
                onPlayProgressInfoUpdateListener.onFinish()
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
    }
}