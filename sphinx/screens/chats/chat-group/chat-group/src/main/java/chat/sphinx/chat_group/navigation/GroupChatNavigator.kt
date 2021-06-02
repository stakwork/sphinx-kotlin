package chat.sphinx.chat_group.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_common.navigation.ChatNavigator
import io.matthewnelson.concept_navigation.BaseNavigationDriver

abstract class GroupChatNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): ChatNavigator(navigationDriver)
{
}