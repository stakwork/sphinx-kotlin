package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_podcast.PodcastModel
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastModelDto(
    val type: String,
    val suggested: Double,
)

fun PodcastModelDto.toPodcastModel(): PodcastModel {
    return PodcastModel(this.type, this.suggested)
}