package chat.sphinx.chat_contact.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_common.navigation.ChatNavigator
import io.matthewnelson.concept_navigation.BaseNavigationDriver

abstract class ContactChatNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): ChatNavigator(navigationDriver)
{
}