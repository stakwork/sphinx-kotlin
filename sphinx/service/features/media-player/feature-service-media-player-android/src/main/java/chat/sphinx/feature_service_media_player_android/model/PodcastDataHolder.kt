package chat.sphinx.feature_service_media_player_android.model

import android.media.MediaPlayer
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat

internal class PodcastDataHolder(
    val chatId: ChatId,
    val episodeId: Long,
    val satsPerMinute: Sat,
    val mediaPlayer: MediaPlayer,
) {
    var speed: Double = 1.0
        private set

    fun setSpeed(speed: Double): Double {
        this.speed = if (speed in 0.5..2.1) {
            speed
        } else {
            1.0
        }
        return this.speed
    }
}
