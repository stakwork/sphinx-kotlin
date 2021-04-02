package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_contact.navigation.ToChatContactScreen
import chat.sphinx.chat_group.navigation.ToChatGroupScreen
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.wrapper_common.chat.ChatId
import javax.inject.Inject

class DashboardNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): DashboardNavigator(navigationDriver)
{
    override suspend fun toChatContact(chatId: ChatId) {
        navigationDriver.submitNavigationRequest(ToChatContactScreen(/*chatId*/))
    }

    override suspend fun toChatGroup(chatId: ChatId) {
        navigationDriver.submitNavigationRequest(ToChatGroupScreen(/*chatId*/))
    }

    override suspend fun toChatTribe(chatId: ChatId) {
        navigationDriver.submitNavigationRequest(ToChatTribeScreen(/*chatId*/))
    }
}