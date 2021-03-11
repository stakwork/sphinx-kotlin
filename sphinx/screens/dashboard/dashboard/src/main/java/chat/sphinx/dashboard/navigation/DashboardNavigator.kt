package chat.sphinx.dashboard.navigation

import androidx.navigation.NavController
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class DashboardNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun toChatContact(chatId: String)
    abstract suspend fun toChatGroup(chatId: String)
    abstract suspend fun toChatTribe(chatId: String)
}
