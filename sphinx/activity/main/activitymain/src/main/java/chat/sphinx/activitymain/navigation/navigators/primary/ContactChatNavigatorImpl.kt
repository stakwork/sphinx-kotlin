package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.send_attachment.navigation.ToSendAttachmentDetail
import javax.inject.Inject

internal class ContactChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): ContactChatNavigator(navigationDriver)
{
}
