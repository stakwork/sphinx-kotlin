package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import java.io.File

interface DownloadableFeedItem {
    val id: FeedId
    val enclosureLength: FeedEnclosureLength?
    val enclosureUrl: FeedUrl
    val enclosureType: FeedEnclosureType?
    val localFile: File?
}