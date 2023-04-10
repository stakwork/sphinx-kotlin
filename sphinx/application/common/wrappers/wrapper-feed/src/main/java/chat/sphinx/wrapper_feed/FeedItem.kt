package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.time
import java.io.File

inline val FeedItem.isPodcast: Boolean
    get() = feed?.feedType?.isPodcast() == true

inline val FeedItem.isVideo: Boolean
    get() = feed?.feedType?.isVideo() == true

inline val FeedItem.isNewsletter: Boolean
    get() = feed?.feedType?.isNewsletter() == true

data class FeedItem(
    override val id: FeedId, // TODO: Should this be FeedItemId?
    val title: FeedTitle,
    val description: FeedDescription?,
    val datePublished: DateTime?,
    val dateUpdated: DateTime?,
    val author: FeedAuthor?,
    val contentType: FeedContentType?,
    override val enclosureLength: FeedEnclosureLength?,
    override val enclosureUrl: FeedUrl,
    override val enclosureType: FeedEnclosureType?,
    val imageUrl: PhotoUrl?,
    val thumbnailUrl: PhotoUrl?,
    val link: FeedUrl?,
    val feedId: FeedId,
    val duration: FeedItemDuration?,
    override var localFile: File?
): DownloadableFeedItem {

    var feed: Feed? = null

    var itemImageUrlToShow: PhotoUrl? = null
        get() {
            imageUrl?.let {
                return it
            }
            return thumbnailUrlToShow
        }

    var imageUrlToShow: PhotoUrl? = null
        get() {
            imageUrl?.let {
                return it
            }
            return feed?.imageUrlToShow
        }

    var thumbnailUrlToShow: PhotoUrl? = null
        get() {
            thumbnailUrl?.let {
                return it
            }
            return null
        }

    var titleToShow: String = ""
        get() = title.value.trim()

    var descriptionToShow: String = ""
        get() {
            return (description?.value ?: feed?.description?.value ?: "").htmlToPlainText().trim()
        }

    var people: ArrayList<String> = arrayListOf()
        get() {
            author?.let {
                return arrayListOf(it.value)
            }
            feed?.author?.let {
                return arrayListOf(it.value)
            }
            return arrayListOf()
        }

    var datePublishedTime: Long = 0
        get() {
            return datePublished?.time ?: 0
        }

    val downloaded: Boolean
        get()= localFile != null

    var contentEpisodeStatus: ContentEpisodeStatus? = null

    var showTitle: String? = null
    var feedType: FeedType? = null
}