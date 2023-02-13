package chat.sphinx.wrapper_feed

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_common.lightning.Sat

data class ContentFeedStatus(
    val feedId: FeedId,
    val feedUrl: FeedUrl,
    val subscriptionStatus: Subscribed,
    val chatId: ChatId?,
    val itemId: FeedId?,
    val satsPerMinute: Sat?,
    val playerSpeed: FeedPlayerSpeed?
) {

    val actualChatId: ChatId?
        get() = if (chatId?.value == ChatId.NULL_CHAT_ID.toLong()) null else chatId
}