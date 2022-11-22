package chat.sphinx.wrapper_feed

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

data class FeedRecommendation(
    val id: String,
    val feedType: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    val title: String,
) {

    companion object {
        const val PODCAST_TYPE = "podcast"
        const val YOUTUBE_VIDEO_TYPE = "youtube"
        const val NEWSLETTER_TYPE = "newsletter"
    }

    val isPodcast: Boolean
        get() = feedType == PODCAST_TYPE

    val isYouTubeVideo: Boolean
        get() = feedType == YOUTUBE_VIDEO_TYPE

    val isNewsletter: Boolean
        get() = feedType == NEWSLETTER_TYPE
}

@JsonClass(generateAdapter = true)
internal data class FeedRecommendationMoshi(
    val id: String,
    val feedType: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    val title: String
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
                it.imageUrl,
                it.link,
                it.title,
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
                imageUrl,
                link,
                title
            )
        )




