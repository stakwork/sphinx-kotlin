package chat.sphinx.threads.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.threads.R
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_message.ThreadUUID
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class ThreadsNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    @JvmSynthetic
    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(
            PopBackStack()
        )
    }

    @JvmSynthetic
    internal suspend fun toChatTribeThread(chatId: ChatId, threadUUID: ThreadUUID) {
        navigationDriver.submitNavigationRequest(
            ToChatTribeScreen(
                chatId = chatId,
                threadUUID = threadUUID,
                popUpToId = R.id.navigation_threads_fragment,
                popUpToInclusive = false,
            )
        )
    }
}
