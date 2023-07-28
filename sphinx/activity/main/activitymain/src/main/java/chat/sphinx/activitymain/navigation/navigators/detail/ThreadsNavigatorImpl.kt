package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.threads.navigation.ThreadsNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_message.ThreadUUID
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import javax.inject.Inject

internal class ThreadsNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    val detailDriver: DetailNavigationDriver,
): ThreadsNavigator(navigationDriver) {

//    override suspend fun popBackStack() {
//        navigationDriver.submitNavigationRequest(PopBackStack())
//    }
}
