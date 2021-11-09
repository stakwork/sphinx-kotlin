package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.PodcastDestination
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastDestinationDto(
    val split: Long,
    val address: String,
    val type: String,
)

fun PodcastDestinationDto.toPodcastDestination(
    podcastId: FeedId
): PodcastDestination {
    return PodcastDestination(
        this.split.toDouble().toFeedDestinationSplit() ?: FeedDestinationSplit(0.0),
        this.address.toFeedDestinationAddress() ?: FeedDestinationAddress("null"),
        this.type.toFeedDestinationType() ?: FeedDestinationType("null"),
        podcastId
    )
}