package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.*
import java.io.File

data class PodcastEpisode(
    override val id: FeedId,
    val title: FeedTitle,
    val description: FeedDescription?,
    val image: PhotoUrl?,
    val link: FeedUrl?,
    val podcastId: FeedId,
    override val enclosureUrl: FeedUrl,
    override val enclosureLength: FeedEnclosureLength?,
    override val enclosureType: FeedEnclosureType?,
    override var localFile: File?,
): DownloadableFeedItem {

    var playing: Boolean = false

    var imageUrlToShow: PhotoUrl? = null
        get() {
            image?.let {
                return it
            }
            return null
        }

    val downloaded: Boolean
        get()= localFile != null

    val episodeUrl: String
        get()= localFile?.toString() ?: enclosureUrl.value
}