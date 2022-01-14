package chat.sphinx.feature_service_media_player_android.model

import android.media.MediaPlayer
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_feed.FeedDestination
import chat.sphinx.wrapper_podcast.PodcastDestination

internal class PodcastDataHolder private constructor(
    val chatId: ChatId,
    val podcastId: String,
    val episodeId: String,
    val mediaPlayer: MediaPlayer,
) {
    companion object {
        @JvmSynthetic
        fun instantiate(
            chatId: ChatId,
            podcastId: String,
            episodeId: String,
            satsPerMinute: Sat,
            mediaPlayer: MediaPlayer,
            speed: Double,
        ): PodcastDataHolder =
            PodcastDataHolder(
                chatId,
                podcastId,
                episodeId,
                mediaPlayer
            ).also {
                it.setSpeed(speed)
                it.setSatsPerMinute(satsPerMinute)
            }
    }

    var speed: Double = 1.0
        private set

    var satsPerMinute: Sat = Sat(0)
        private set

    var destinations: List<FeedDestination> = ArrayList(0)
        private set

    fun setSpeed(speed: Double): Double {
        this.speed = if (speed in 0.5..2.1) {
            speed
        } else {
            1.0
        }
        return this.speed
    }

    fun setSatsPerMinute(sats: Sat): Sat {
        this.satsPerMinute = sats
        return this.satsPerMinute
    }

    fun setDestinations(destinations: List<FeedDestination>) {
        this.destinations = destinations
    }

    val currentTimeSeconds: Int
        get() = mediaPlayer.currentPosition / 1000

    val currentTimeMilliSeconds: Int
        get() = mediaPlayer.currentPosition

    val durationMilliSeconds: Int
        get() = mediaPlayer.duration
}
