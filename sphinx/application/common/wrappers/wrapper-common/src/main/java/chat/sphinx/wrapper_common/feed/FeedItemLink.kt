package chat.sphinx.wrapper_common.feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedItemLink(): FeedItemLink? =
    try {
        FeedItemLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }
@Suppress("NOTHING_TO_INLINE")
inline val String.isValidFeedItemLink: Boolean
    get() = isNotEmpty() && matches("^${FeedItemLink.REGEX}\$".toRegex())

@Suppress("NOTHING_TO_INLINE")
inline fun generateFeedItemLink(
    feedUrl: FeedUrl,
    feedId: FeedId,
    itemId: FeedId,
    atTime: Long?
): String {
    val feedUrlComponent = "feedURL=${feedUrl.value}"
    val feedIdComponent = "feedID=${feedId.value}"
    val itemIdComponent = "itemID=${itemId.value}"
    val atTimeComponent = atTime?.let { "atTime=$it" }

    val components = mutableListOf(feedUrlComponent, feedIdComponent, itemIdComponent)
    atTimeComponent?.let { components.add(it) }

    return "sphinx.chat://?action=share_content&${components.joinToString("&")}"
}
@JvmInline
value class FeedItemLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=share_content&feedURL=.*"
        const val FEED_URL = "feedURL"
        const val FEED_ID = "feedID"
        const val ITEM_ID = "itemID"
        const val AT_TIME = "atTime"
    }

    init {
        require(value.isValidFeedItemLink) {
            "Invalid Tribe Join Link"
        }
    }

    inline val feedUrl : String
        get() = (getComponent(FEED_URL) ?: "").trim()

    inline val feedId : String
        get() = (getComponent(FEED_ID) ?: "").trim()

    inline val itemId : String
        get() = (getComponent(ITEM_ID) ?: "").trim()

    inline val atTime : String?
        get() = (getComponent(AT_TIME))?.trim()

    fun getComponent(k: String): String? {
        val components = value.replace("sphinx.chat://", "").split("&")
        for (component in components) {
            val subComponents = component.split("=")
            val key:String? = if (subComponents.isNotEmpty()) subComponents.elementAtOrNull(0) else null
            val value:String? = if (subComponents.size > 1) subComponents.elementAtOrNull(1) else null

            if (key == k) return value
        }
        return null
    }

}