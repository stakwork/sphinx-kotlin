package chat.sphinx.wrapper_common.feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedUrl(): FeedUrl? =
    try {
        FeedUrl(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun FeedUrl.isYoutubeVideo(): Boolean =
    value.contains("www.youtube.com") || value.contains("https://youtu.be/")

@JvmInline
value class FeedUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedUrl cannot be empty"
        }
    }

    companion object {
        const val YOUTUBE_WEB_VIEW_MIME_TYPE = "text/html"
        const val YOUTUBE_WEB_VIEW_ENCODING = "utf-8"
        const val YOUTUBE_WEB_VIEW_IFRAME_PATTERN = "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube-nocookie.com/embed/%s\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>"
    }
}
