package chat.sphinx.concept_network_query_chat.model.feed

import chat.sphinx.wrapper_common.feed.isYoutubeVideo
import chat.sphinx.wrapper_common.feed.toFeedUrl
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedDto(
    val id: String,
    val feedType: Long,
    val title: String,
    val url: String,
    val description: String?,
    val author: String?,
    val generator: String?,
    val imageUrl: String?,
    val ownerUrl: String?,
    val link: String?,
    val datePublished: Long?,
    val dateUpdated: Long?,
    val contentType: String?,
    val language: String?,
    val value: FeedValueDto?,
    val items: List<FeedItemDto>,
) {

    val fixedId: String
        get() {
            if (url.toFeedUrl()?.isYoutubeVideo() == true) {
                val playlistID = url.substringAfter("?playlist_id=", "")
                val channelID = url.substringAfter("?channel_id=", "")

                if (playlistID.isNotEmpty()) {
                    return playlistID
                } else if (channelID.isNotEmpty()) {
                    return channelID
                }
            }
            return id
        }
}