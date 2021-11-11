package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedTitle

data class PodcastEpisode(
    val id: FeedId,
    val title: FeedTitle,
    val description: FeedDescription?,
    val image: PhotoUrl?,
    val link: FeedUrl?,
    val enclosureUrl: FeedUrl,
    val podcastId: FeedId,
) {

    var playing: Boolean = false

    var downloaded: Boolean = false
}