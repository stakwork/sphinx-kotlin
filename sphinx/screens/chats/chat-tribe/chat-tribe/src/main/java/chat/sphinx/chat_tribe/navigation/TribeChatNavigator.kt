package chat.sphinx.chat_tribe.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.BaseNavigationDriver

abstract class TribeChatNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): ChatNavigator(navigationDriver)
{
    abstract suspend fun toPodcastPlayerScreen(
        chatId: ChatId,
        feedId: FeedId,
        feedUrl: FeedUrl,
        fromDownloadedSection: Boolean = false
    )

    abstract suspend fun toTribeDetailScreen(chatId: ChatId)

    abstract suspend fun toShareTribeScreen(
        qrText: String,
        viewTitle: String,
        description: String? = null,
    )

    abstract suspend fun toNotificationsLevel(chatId: ChatId)

    abstract suspend fun toKnownBadges(badgeIds: Array<String>)

    abstract suspend fun toThreads(chatId: ChatId)
}
