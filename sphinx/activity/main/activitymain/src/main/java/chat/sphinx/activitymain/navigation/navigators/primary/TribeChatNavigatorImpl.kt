package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import javax.inject.Inject

class TribeChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): TribeChatNavigator(navigationDriver)
{
}
