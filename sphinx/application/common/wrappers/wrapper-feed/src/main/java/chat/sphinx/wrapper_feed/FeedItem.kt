package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.feed.FeedUrl

inline val FeedItem.isPodcast: Boolean
    get() = feed?.feedType?.isPodcast() == true

inline val FeedItem.isVideo: Boolean
    get() = feed?.feedType?.isVideo() == true

inline val FeedItem.isNewsletter: Boolean
    get() = feed?.feedType?.isNewsletter() == true

data class FeedItem(
    val id: FeedId,
    val title: FeedTitle,
    val description: FeedDescription?,
    val datePublished: DateTime?,
    val dateUpdated: DateTime?,
    val author: FeedAuthor?,
    val contentType: FeedContentType?,
    val enclosureLength: FeedEnclosureLength?,
    val enclosureUrl: FeedUrl,
    val enclosureType: FeedEnclosureType?,
    val imageUrl: PhotoUrl?,
    val thumbnailUrl: PhotoUrl?,
    val link: FeedUrl?,
    val feedId: FeedId
) {

    var feed: Feed? = null

}