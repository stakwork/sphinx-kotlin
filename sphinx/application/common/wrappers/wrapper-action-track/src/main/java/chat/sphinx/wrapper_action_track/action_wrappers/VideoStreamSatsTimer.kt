package chat.sphinx.wrapper_action_track.action_wrappers

import java.util.*

class VideoStreamSatsTimer(
    val onStreamSats: () -> Unit
) {
    private val timer = Timer()
    private lateinit var timerTask: TimerTask
    private var currentTimeWatched: Long = 0

    fun startTimer() {
        timerTask = object : TimerTask() {
            override fun run() {
                currentTimeWatched++

                if (currentTimeWatched >= 60000L) {
                    onStreamSats()
                    currentTimeWatched = 0
                }
            }
        }
        timer.scheduleAtFixedRate(timerTask, 0, 1)
    }

    fun stopTimer(){
        if (this::timerTask.isInitialized) {
            timerTask.cancel()
        }
    }

}