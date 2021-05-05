package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import javax.inject.Inject

class GroupChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): GroupChatNavigator(navigationDriver)
{
}
