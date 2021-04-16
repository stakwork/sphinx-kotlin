package chat.sphinx.dashboard.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class DashboardNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun toChatContact(chat: Chat?, contact: Contact)
    abstract suspend fun toChatGroup(chat: Chat)
    abstract suspend fun toChatTribe(chat: Chat)
}
