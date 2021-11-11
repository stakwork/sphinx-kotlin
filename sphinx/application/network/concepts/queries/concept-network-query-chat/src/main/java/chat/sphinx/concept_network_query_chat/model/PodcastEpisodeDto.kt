package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastEpisodeDto(
    val id: Long,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val enclosureUrl: String,
)

fun PodcastEpisodeDto.toPodcastEpisode(
    podcastId: FeedId
): PodcastEpisode {
    return PodcastEpisode(
        id = id.toString().toFeedId() ?: FeedId("null"),
        title = title.toFeedTitle() ?: FeedTitle("null"),
        description = description.toFeedDescription(),
        image = image.toPhotoUrl(),
        link = link.toFeedUrl(),
        enclosureUrl = enclosureUrl.toFeedUrl() ?: FeedUrl("null"),
        podcastId
    )
}