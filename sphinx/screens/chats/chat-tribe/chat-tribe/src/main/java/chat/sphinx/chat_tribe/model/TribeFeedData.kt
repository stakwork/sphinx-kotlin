package chat.sphinx.chat_tribe.model

import chat.sphinx.wrapper_chat.AppUrl
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl

sealed class TribeFeedData {
    object Loading: TribeFeedData()

    sealed class Result: TribeFeedData() {
        object NoFeed : Result()

        data class FeedData(
            val host: ChatHost,
            val feedUrl: FeedUrl?,
            val chatUUID: ChatUUID,
            val feedType: FeedType,
            val appUrl: AppUrl?,
            val badges: Array<String>,
        ) : Result()
    }
}
