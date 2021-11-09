package chat.sphinx.wrapper_feed

@Suppress("NOTHING_TO_INLINE")
inline fun FeedType.isPodcast(): Boolean =
    this is FeedType.Podcast

@Suppress("NOTHING_TO_INLINE")
inline fun FeedType.isVideo(): Boolean =
    this is FeedType.Video

@Suppress("NOTHING_TO_INLINE")
inline fun FeedType.isNewsletter(): Boolean =
    this is FeedType.Newsletter

sealed class FeedType {

    companion object {
        const val PODCAST = 0 // SHOW
        const val VIDEO = 1
        const val NEWSLETTER = 2 // SHOW
    }

    object Podcast : FeedType()
    object Video : FeedType()
    object Newsletter : FeedType()
}