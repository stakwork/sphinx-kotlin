package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import javax.inject.Inject

class ContactChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): ContactChatNavigator(navigationDriver)
{
}
