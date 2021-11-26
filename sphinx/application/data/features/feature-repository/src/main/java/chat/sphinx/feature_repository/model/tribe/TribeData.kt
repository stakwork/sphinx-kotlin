package chat.sphinx.feature_repository.model.tribe

import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.FeedType

data class TribeData(
    private val host : ChatHost,
    private val chatUUID: ChatUUID,
    private val feedUrl : FeedUrl,
    private val feedType: FeedType,
)