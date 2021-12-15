package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedTitle
import java.io.File

data class PodcastEpisode(
    val id: FeedId,
    val title: FeedTitle,
    val description: FeedDescription?,
    val image: PhotoUrl?,
    val link: FeedUrl?,
    val enclosureUrl: FeedUrl,
    val podcastId: FeedId,
    val localFile: File?
) {

    var playing: Boolean = false

    val downloaded: Boolean
        get()= localFile != null
}