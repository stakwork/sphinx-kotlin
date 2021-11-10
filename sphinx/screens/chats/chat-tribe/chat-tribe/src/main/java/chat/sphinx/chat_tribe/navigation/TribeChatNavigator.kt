package chat.sphinx.chat_tribe.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.BaseNavigationDriver

abstract class TribeChatNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): ChatNavigator(navigationDriver)
{
    abstract suspend fun toPodcastPlayerScreen(chatId: ChatId, currentEpisodeDuration: Long)
    abstract suspend fun toTribeDetailScreen(chatId: ChatId)
}
