package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_contact.navigation.ToChatContactScreen
import chat.sphinx.chat_group.navigation.ToChatGroupScreen
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_contact.Contact
import javax.inject.Inject

class DashboardNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): DashboardNavigator(navigationDriver)
{
    override suspend fun toChatContact(chat: Chat?, contact: Contact) {
        navigationDriver.submitNavigationRequest(
            ToChatContactScreen(chat, contact)
        )
    }

    override suspend fun toChatGroup(chat: Chat) {
        navigationDriver.submitNavigationRequest(
            ToChatGroupScreen(chat)
        )
    }

    override suspend fun toChatTribe(chat: Chat) {
        navigationDriver.submitNavigationRequest(
            ToChatTribeScreen(chat)
        )
    }
}
