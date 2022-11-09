package chat.sphinx.wrapper_action_track

@Suppress("NOTHING_TO_INLINE")
inline fun ActionTrackType.isMessage(): Boolean =
    this is ActionTrackType.Message

@Suppress("NOTHING_TO_INLINE")
inline fun ActionTrackType.isFeedSearch(): Boolean =
    this is ActionTrackType.FeedSearch

@Suppress("NOTHING_TO_INLINE")
inline fun ActionTrackType.isContentBoost(): Boolean =
    this is ActionTrackType.ContentBoost

@Suppress("NOTHING_TO_INLINE")
inline fun ActionTrackType.isPodcastClipComment(): Boolean =
    this is ActionTrackType.PodcastClipComment

@Suppress("NOTHING_TO_INLINE")
inline fun ActionTrackType.isContentConsumed(): Boolean =
    this is ActionTrackType.ContentConsumed

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toActionTrackType(): ActionTrackType =
    when (this) {
        ActionTrackType.MESSAGE -> {
            ActionTrackType.Message
        }
        ActionTrackType.FEED_SEARCH -> {
            ActionTrackType.FeedSearch
        }
        ActionTrackType.CONTENT_BOOST -> {
            ActionTrackType.ContentBoost
        }
        ActionTrackType.PODCAST_CLIP_COMMENT -> {
            ActionTrackType.PodcastClipComment
        }
        ActionTrackType.CONTENT_CONSUMED -> {
            ActionTrackType.ContentConsumed
        }
        else -> {
            ActionTrackType.Unknown(this)
        }
    }

sealed class ActionTrackType {

    companion object {
        const val MESSAGE = 0
        const val FEED_SEARCH = 1
        const val CONTENT_BOOST = 2
        const val PODCAST_CLIP_COMMENT = 3
        const val CONTENT_CONSUMED = 4
    }

    abstract val value: Int

    object Message : ActionTrackType() {
        override val value: Int
            get() = MESSAGE
    }

    object FeedSearch : ActionTrackType() {
        override val value: Int
            get() = FEED_SEARCH
    }

    object ContentBoost : ActionTrackType() {
        override val value: Int
            get() = CONTENT_BOOST
    }

    object PodcastClipComment : ActionTrackType() {
        override val value: Int
            get() = PODCAST_CLIP_COMMENT
    }

    object ContentConsumed : ActionTrackType() {
        override val value: Int
            get() = CONTENT_CONSUMED
    }

    data class Unknown(override val value: Int) : ActionTrackType()
}