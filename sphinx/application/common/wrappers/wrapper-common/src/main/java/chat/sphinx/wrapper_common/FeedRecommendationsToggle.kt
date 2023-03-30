package chat.sphinx.wrapper_common

sealed class FeedRecommendationsToggle {

    companion object {
        const val ENABLED = 1
        const val DISABLED = 0

        const val FEED_RECOMMENDATIONS_SHARED_PREFERENCES = "general_settings"
        const val FEED_RECOMMENDATIONS_ENABLED_KEY = "feed-recommendations-enabled"
    }

    abstract val value: Int

    object True: FeedRecommendationsToggle() {
        override val value: Int
            get() = ENABLED
    }

    object False: FeedRecommendationsToggle() {
        override val value: Int
            get() = DISABLED
    }
}
