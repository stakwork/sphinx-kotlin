package chat.sphinx.activitymain.navigation

import chat.sphinx.chat_contact.navigation.ToChatContactScreen
import chat.sphinx.chat_group.navigation.ToChatGroupScreen
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.dashboard.navigation.DashboardNavigator
import javax.inject.Inject

class DashboardNavigatorImpl @Inject constructor(
    navigationDriver: MainNavigationDriver
): DashboardNavigator(navigationDriver)
{
    override suspend fun toChatContact(chatId: String) {
        navigationDriver.submitNavigationRequest(ToChatContactScreen(/*chatId*/))
    }

    override suspend fun toChatGroup(chatId: String) {
        navigationDriver.submitNavigationRequest(ToChatGroupScreen(/*chatId*/))
    }

    override suspend fun toChatTribe(chatId: String) {
        navigationDriver.submitNavigationRequest(ToChatTribeScreen(/*chatId*/))
    }
}