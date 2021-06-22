package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.send_attachment.navigation.ToSendAttachmentDetail
import javax.inject.Inject

internal class GroupChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): GroupChatNavigator(navigationDriver)
{
}
