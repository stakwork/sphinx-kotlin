package chat.sphinx.chat_tribe.model

import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.FeedType

sealed class TribeFeedData {
    object Loading: TribeFeedData()

    sealed class Result: TribeFeedData() {
        object NoFeed : Result()

        data class FeedData(
            val host: ChatHost,
            val feedUrl: FeedUrl,
            val feedType: FeedType,
            val metaData: ChatMetaData?,
        ) : Result()
    }
}
