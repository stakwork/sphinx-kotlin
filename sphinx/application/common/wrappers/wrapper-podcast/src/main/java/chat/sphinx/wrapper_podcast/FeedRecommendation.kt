package chat.sphinx.wrapper_podcast

import java.util.*

data class FeedRecommendation(
    val id: String,
    val pubKey: String?,
    val feedType: String,
    val description: String,
    val smallImageUrl: String?,
    val mediumImageUrl: String?,
    val largeImageUrl: String?,
    val link: String,
    val title: String,
    val showTitle: String,
    val date: Long?,
    val timestamp: String,
    val topics: List<String>,
    val guests: List<String>,
    val position: Int
) {

    companion object {
        const val PODCAST_TYPE = "podcast"
        const val YOUTUBE_VIDEO_TYPE = "youtube"
        const val TWITTER_TYPE = "twitter_space"

        const val RECOMMENDATION_PODCAST_ID = "Recommendations-Feed"
        const val RECOMMENDATION_PODCAST_TITLE = "Recommendations"
        const val RECOMMENDATION_PODCAST_DESCRIPTION = "Feed Recommendations"
    }

    val largestImageUrl: String?
        get() = largeImageUrl ?: mediumImageUrl ?: smallImageUrl

    val isTwitterSpace: Boolean
        get() = feedType == TWITTER_TYPE

    val isPodcast: Boolean
        get() = feedType == PODCAST_TYPE

    val isYouTubeVideo: Boolean
        get() = feedType == YOUTUBE_VIDEO_TYPE

    val isMusicClip: Boolean
        get() = feedType == PODCAST_TYPE || feedType == TWITTER_TYPE


    val startMilliseconds: Int
        get() {
            return timestamp.split("-").firstOrNull()?.toMilliseconds() ?: 0
        }

    val endMilliseconds: Int
        get() {
            return timestamp.split("-").lastOrNull()?.toMilliseconds() ?: 0
        }

    val daysAgo: String
        get() {
            date?.let { nnDate ->
                return if (nnDate > 0) {
                    Date(nnDate).getTimeAgo()
                } else ""
            }
            return ""
        }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMilliseconds(): Int {
    val elements = this.split(":")
    if (elements.size == 3) {
        val hours = elements[0].toIntOrNull() ?: 0
        val minutes = elements[1].toIntOrNull() ?: 0
        val seconds = elements[2].toIntOrNull() ?: 0
        return (seconds * 1000) + (minutes * 60 * 1000) + (hours * 60 * 60 * 1000)
    }
    return 0
}

inline fun Date.getTimeAgo(): String {

    val secondsMillis = 1000
    val minuteMillis = 60 * secondsMillis
    val hourMillis = 60 * minuteMillis
    val dayMillis = 24 * hourMillis

    var time = this.time
    if (time < 1000000000000L) {
        time *= 1000
    }

    val now = Calendar.getInstance().time.time
    if (time > now || time <= 0) {
        return "in the future"
    }

    val diff = now - time
    return when {
        diff < minuteMillis -> "moments ago"
        diff < 2 * minuteMillis -> "a minute ago"
        diff < 60 * minuteMillis -> "${diff / minuteMillis} minutes ago"
        diff < 2 * hourMillis -> "an hour ago"
        diff < 24 * hourMillis -> "${diff / hourMillis} hours ago"
        diff < 48 * hourMillis -> "yesterday"
        else -> "${diff / dayMillis} days ago"
    }
}





