package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.PodcastModel
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastModelDto(
    val type: String,
    val suggested: Double,
)

fun PodcastModelDto.toPodcastModel(
    podcastId: FeedId
): PodcastModel {
    return PodcastModel(
        this.type.toFeedModelType() ?: FeedModelType("null"),
        this.suggested.toFeedModelSuggested() ?: FeedModelSuggested(0.0),
        podcastId
    )
}