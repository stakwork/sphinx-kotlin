package chat.sphinx.wrapper_podcast

data class FeedRecommendation(
    val id: String,
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





