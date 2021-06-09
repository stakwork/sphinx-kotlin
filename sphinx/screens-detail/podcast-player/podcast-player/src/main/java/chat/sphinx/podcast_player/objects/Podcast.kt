package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.FeedId
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Podcast(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val value: PodcastValue,
    val episodes: List<PodcastEpisode>,
): Parcelable {

    var episodeId: Long? = null
    var timeSeconds: Int? = null
    var speed: Double? = null

    fun setMetaData(metaData: ChatMetaData) {
        this.episodeId = metaData.itemId.value
        this.timeSeconds = metaData.timeSeconds
        this.speed = metaData.speed
    }

    fun getCurrentEpisode(): PodcastEpisode {
        episodeId?.let { episodeId ->
            for (episode in episodes) {
                if (episode.id == episodeId) {
                    return episode
                }
            }
        }
        return episodes[0]
    }

    fun getCurrentTime(): Int {
        timeSeconds?.let { time ->
            return time
        }
        return 0
    }
}

fun PodcastDto.toPodcast(): Podcast {
    val podcastEpisodes = mutableListOf<PodcastEpisode>()

    for (episode in this.episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    return Podcast(this.id, this.title,this.description,this.author,this.image, this.value.toPodcastValue(), podcastEpisodes)
}