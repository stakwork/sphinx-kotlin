package chat.sphinx.wrapper_chat

import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.message.MessageUUID

data class TribeData(
    val host : ChatHost,
    val chatUUID: ChatUUID,
    val feedUrl: FeedUrl?,
    val feedType: FeedType,
    val pin: MessageUUID?,
    val appUrl: AppUrl?,
    val badges: Array<String>
)