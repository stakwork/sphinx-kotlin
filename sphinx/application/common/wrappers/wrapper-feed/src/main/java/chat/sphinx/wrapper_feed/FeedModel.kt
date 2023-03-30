package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.feed.FeedId

data class FeedModel(
    val id: FeedId,
    val type: FeedModelType,
    val suggested: FeedModelSuggested
) {
    companion object {
        private const val satsInBTC = 100_000_000
    }

    val suggestedSats: Long
        get() = (suggested.value * satsInBTC.toDouble()).toLong()
}