package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastDto(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val value: PodcastValueDto,
    val episodes: List<PodcastEpisodeDto>,
){

    var episodeId: Long? = null
    var timeSeconds: Int? = null
    var speed: Double? = null

    fun setMetaData(metaData: ChatMetaData) {
        this.episodeId = metaData.itemId.value
        this.timeSeconds = metaData.timeSeconds
        this.speed = metaData.speed
    }

    fun isValidPodcast() : Boolean {
        return episodes.isNotEmpty() && title.isNotEmpty()
    }

    fun getCurrentEpisode(): PodcastEpisodeDto {
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