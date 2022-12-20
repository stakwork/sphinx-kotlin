package chat.sphinx.wrapper_action_track.action_wrappers

import chat.sphinx.wrapper_common.feed.FeedId
import java.util.*

class VideoRecordConsumed(
    val feedItemId: FeedId
) {
    private val timer = Timer()
    private lateinit var timerTask: TimerTask
    private var currentTimeWatched: Long = 0
    private var startTimeStamp: Long = 0
    var history: ArrayList<ContentConsumedHistoryItem> = arrayListOf()

    fun startTimer(){
        timerTask = object : TimerTask() {
            override fun run() {
                currentTimeWatched++
            }
        }
        timer.scheduleAtFixedRate(timerTask, 0, 1)
    }

    fun stopTimer(){
        if (this::timerTask.isInitialized) {
            timerTask.cancel()
        }
    }

    fun createHistoryItem(){
        if (currentTimeWatched == 0L){
            return
        }
        val endTimeStamp = startTimeStamp + currentTimeWatched
        val item = ContentConsumedHistoryItem(
            topics = arrayListOf(""),
            startTimeStamp,
            endTimeStamp,
            Date().time
        )
        history.add(item)
        currentTimeWatched = 0
    }

    fun setNewHistoryItem(videoPosition: Long){
        createHistoryItem()
        currentTimeWatched = 0
        startTimeStamp = videoPosition
    }
}

