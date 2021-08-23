package chat.sphinx.chat_tribe.model

import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_chat.FeedUrl

sealed class TribePodcastData {
    object Loading: TribePodcastData()

    sealed class Result: TribePodcastData() {
        object NoPodcast : Result()

        data class TribeData(
            val host: ChatHost,
            val feedUrl: FeedUrl,
            val metaData: ChatMetaData?,
        ) : Result()
    }
}
