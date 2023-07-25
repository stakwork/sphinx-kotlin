package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_message.ThreadUUID
import javax.inject.Inject

internal class ThreadsNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
): ThreadsNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

}
