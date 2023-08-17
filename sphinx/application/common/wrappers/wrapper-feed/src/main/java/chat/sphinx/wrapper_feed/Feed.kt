package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.lightning.toSat

inline val Feed.isPodcast: Boolean
    get() = feedType.isPodcast()

inline val Feed.isVideo: Boolean
    get() = feedType.isVideo()

inline val Feed.isNewsletter: Boolean
    get() = feedType.isNewsletter()

data class Feed(
    val id: FeedId,
    val feedType: FeedType,
    val title: FeedTitle,
    val description: FeedDescription?,
    val feedUrl: FeedUrl,
    val author: FeedAuthor?,
    val generator: FeedGenerator?,
    val imageUrl: PhotoUrl?,
    val ownerUrl: FeedUrl?,
    val link: FeedUrl?,
    val datePublished: DateTime?,
    val dateUpdated: DateTime?,
    val contentType: FeedContentType?,
    val language: FeedLanguage?,
    val itemsCount: FeedItemsCount,
    val currentItemId: FeedId?, //Not used anymore. Replaced by contentFeedEpisodeStatus.itemId
    val chatId: ChatId,
    val subscribed: Subscribed,
    val lastPlayed: DateTime?
) {

    companion object {
        const val TRIBES_DEFAULT_SERVER_URL = "tribes.sphinx.chat"

        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    override fun equals(other: Any?): Boolean {
        return  other                                             is Feed                                        &&
                other.id                                          == id                                          &&
                other.feedType                                    == feedType                                    &&
                other.title                                       == title                                       &&
                other.description                                 == description                                 &&
                other.feedUrl                                     == feedUrl                                     &&
                other.author                                      == author                                      &&
                other.generator                                   == generator                                   &&
                other.imageUrl                                    == imageUrl                                    &&
                other.ownerUrl                                    == ownerUrl                                    &&
                other.link                                        == link                                        &&
                other.datePublished                               == datePublished                               &&
                other.dateUpdated                                 == dateUpdated                                 &&
                other.contentType                                 == contentType                                 &&
                other.language                                    == language                                    &&
                other.itemsCount                                  == itemsCount                                  &&
                other.currentItemId                               == currentItemId                               &&
                other.chatId                                      == chatId                                      &&
                other.subscribed                                  == subscribed                                  &&
                other.lastItem?.contentEpisodeStatus?.duration    == lastItem?.contentEpisodeStatus?.duration    &&
                other.lastItem?.contentEpisodeStatus?.currentTime == lastItem?.contentEpisodeStatus?.currentTime &&
                other.lastPlayed                                  == lastPlayed
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + id.hashCode()
        result = _31 * result + feedType.hashCode()
        result = _31 * result + title.hashCode()
        result = _31 * result + description.hashCode()
        result = _31 * result + feedUrl.hashCode()
        result = _31 * result + author.hashCode()
        result = _31 * result + generator.hashCode()
        result = _31 * result + imageUrl.hashCode()
        result = _31 * result + ownerUrl.hashCode()
        result = _31 * result + link.hashCode()
        result = _31 * result + datePublished.hashCode()
        result = _31 * result + dateUpdated.hashCode()
        result = _31 * result + contentType.hashCode()
        result = _31 * result + language.hashCode()
        result = _31 * result + itemsCount.hashCode()
        result = _31 * result + currentItemId.hashCode()
        result = _31 * result + chatId.hashCode()
        result = _31 * result + subscribed.hashCode()
        result = _31 * result + lastItem?.contentEpisodeStatus?.duration.hashCode()
        result = _31 * result + lastItem?.contentEpisodeStatus?.currentTime.hashCode()
        result = _31 * result + lastPlayed.hashCode()
        return result
    }

    var items: List<FeedItem> = listOf()

    var destinations: List<FeedDestination> = listOf()

    var model: FeedModel? = null

    var chat: Chat? = null

    var contentFeedStatus: ContentFeedStatus? = null

    fun getNNContentFeedStatus() : ContentFeedStatus {
        contentFeedStatus?.let {
            return it
        }

        var itemId = chat?.metaData?.itemId ?: currentItemId
        itemId = if (itemId?.value == FeedId.NULL_FEED_ID) null else itemId

        val chatId = if (this.chat?.id?.value == ChatId.NULL_CHAT_ID.toLong()) null else this.chat?.id

        val satsPerMinute = chat?.metaData?.satsPerMinute ?: model?.suggestedSats?.toSat()

        return ContentFeedStatus(
            id,
            feedUrl,
            this.subscribed,
            chatId,
            itemId,
            satsPerMinute,
            chat?.metaData?.speed?.toFeedPlayerSpeed()
        )

    }

    var lastPublished: FeedItem? = null
        get() {
            if (items.isNotEmpty()) {
                return items.first()
            }
            return null
        }

    var lastItem: FeedItem? = null
        get() {
            if (items.isNotEmpty()) {
                return items.first()
            }
            return null
        }

    var imageUrlToShow: PhotoUrl? = null
        get() {
            imageUrl?.let {
                return it
            }
            chat?.photoUrl?.let {
                return it
            }
            return null
        }

    var titleToShow: String = ""
        get() = title.value.trim()

    var descriptionToShow: String = ""
        get() {
            return (description?.value ?: "").htmlToPlainText().trim()
        }

    val hasDestinations: Boolean
        get() = destinations.isNotEmpty()
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.htmlToPlainText(): String =
    this.replace(Regex("\\<[^>]*>"),"")