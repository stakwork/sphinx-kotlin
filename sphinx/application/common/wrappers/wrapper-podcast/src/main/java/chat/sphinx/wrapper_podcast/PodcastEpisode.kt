package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.time
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.FeedRecommendation.Companion.PODCAST_TYPE
import chat.sphinx.wrapper_podcast.FeedRecommendation.Companion.TWITTER_TYPE
import chat.sphinx.wrapper_podcast.FeedRecommendation.Companion.YOUTUBE_VIDEO_TYPE
import java.io.File
import java.util.*

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
    val date: DateTime?,
    val feedType: String = PODCAST_TYPE,
    val showTitle: FeedTitle? = null,
    val clipStartTime: Int? = null,
    val clipEndTime: Int? = null,
    val topics: List<String> = listOf(),
    val people: List<String> = listOf()
): DownloadableFeedItem {

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    override fun equals(other: Any?): Boolean {
        if (other is PodcastEpisode) {
            return other.id == id
        }
        return false
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + id.hashCode()
        return result
    }

    var titleToShow: String = ""
        get() = title.value.trim()

    var showTitleToShow: String = ""
        get() = showTitle?.value?.trim() ?: ""

    var descriptionToShow: String = ""
        get() {
            return (description?.value ?: "").htmlToPlainText().trim()
        }

    var playing: Boolean = false

    var imageUrlToShow: PhotoUrl? = null
        get() {
            image?.let {
                return it
            }
            return null
        }

    val dateString: String
        get() {
            return date?.value?.let {
                DateTime.getFormatMMMddyyyy(
                    TimeZone.getTimeZone(DateTime.UTC)
                ).format(it)
            } ?: "-"
        }

    val downloaded: Boolean
        get()= localFile != null

    val episodeUrl: String
        get()= localFile?.toString() ?: enclosureUrl.value

    val isTwitterSpace: Boolean
        get() = feedType == TWITTER_TYPE

    val isPodcast: Boolean
        get() = feedType == PODCAST_TYPE

    val isYouTubeVideo: Boolean
        get() = feedType == YOUTUBE_VIDEO_TYPE

    val isMusicClip: Boolean
        get() = feedType == PODCAST_TYPE || feedType == TWITTER_TYPE

    val longType: Long
        get() {
            when {
                isMusicClip -> {
                    return FeedType.PODCAST.toLong()
                }
                isYouTubeVideo -> {
                    return FeedType.VIDEO.toLong()
                }
            }
            return FeedType.PODCAST.toLong()
        }

    var datePublishedTime: Long = 0
        get() {
            return date?.time ?: 0
        }
}