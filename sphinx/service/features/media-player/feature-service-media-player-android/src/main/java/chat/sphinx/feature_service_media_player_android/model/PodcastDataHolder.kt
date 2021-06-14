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
}
