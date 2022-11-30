package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.chatTimeFormat
import chat.sphinx.wrapper_common.toDateTime
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import java.io.File

data class FeedRecommendation(
    val id: String,
    val feedType: String,
    val description: String,
    val smallImageUrl: String?,
    val mediumImageUrl: String?,
    val largeImageUrl: String?,
    val link: String,
    val title: String,
    val date: Long?,
    val position: Int,
) {

    companion object {
        const val PODCAST_TYPE = "podcast"
        const val YOUTUBE_VIDEO_TYPE = "youtube"
        const val NEWSLETTER_TYPE = "newsletter"
    }

    val largestImageUrl: String?
        get() = largeImageUrl ?: mediumImageUrl ?: smallImageUrl

    val smallestImageUrl: String?
        get() = smallImageUrl ?: mediumImageUrl ?: largeImageUrl

    val isPodcast: Boolean
        get() = feedType == PODCAST_TYPE

    val isYouTubeVideo: Boolean
        get() = feedType == YOUTUBE_VIDEO_TYPE

    val isNewsletter: Boolean
        get() = feedType == NEWSLETTER_TYPE

    val dateString: String
        get() = date?.toDateTime()?.chatTimeFormat() ?: "-"

    var duration: Long? = null

    private var currentTimeMilliseconds: Int? = null

    val currentTime: Int
        get() = currentTimeMilliseconds ?: 0

    var isPlaying: Boolean = false

    fun resetPlayerData() {
        isPlaying = false
        currentTimeMilliseconds = 0
    }

    fun getDuration(
        durationRetrieverHandler: (url: String, localFile: File?) -> Long
    ): Long {
        if (duration == null) {

            duration = durationRetrieverHandler(
                link,
                null
            )
        }

        return duration ?: 0
    }

    fun playingItemUpdate(time: Int, duration: Long) {
        this.duration = if (duration > 0) duration else this.duration
        this.currentTimeMilliseconds = time
        this.isPlaying = true
    }

    fun pauseItemUpdate() {
        this.isPlaying = false
    }

    fun endEpisodeUpdate() {
        this.isPlaying = false
        this.currentTimeMilliseconds = 0
    }
}

@JsonClass(generateAdapter = true)
internal data class FeedRecommendationMoshi(
    val id: String,
    val feedType: String,
    val description: String,
    val smallImageUrl: String?,
    val mediumImageUrl: String?,
    val largeImageUrl: String?,
    val link: String,
    val title: String,
    val date: Long?,
    val position: Int
)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedRecommendationOrNull(moshi: Moshi): FeedRecommendation? =
    try {
        this.toFeedRecommendation(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toFeedRecommendation(moshi: Moshi): FeedRecommendation =
    moshi.adapter(FeedRecommendationMoshi::class.java)
        .fromJson(this)
        ?.let {
            FeedRecommendation(
                it.id,
                it.feedType,
                it.description,
                it.smallImageUrl,
                it.mediumImageUrl,
                it.largeImageUrl,
                it.link,
                it.title,
                it.date,
                it.position
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun FeedRecommendation.toJson(moshi: Moshi): String =
    moshi.adapter(FeedRecommendationMoshi::class.java)
        .toJson(
            FeedRecommendationMoshi(
                id,
                feedType,
                description,
                smallImageUrl,
                mediumImageUrl,
                largeImageUrl,
                link,
                title,
                date,
                position
            )
        )




