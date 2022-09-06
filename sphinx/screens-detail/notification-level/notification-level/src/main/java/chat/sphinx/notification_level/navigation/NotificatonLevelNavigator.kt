package chat.sphinx.notification_level.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class NotificationLevelNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun closeDetailScreen()

    abstract suspend fun toNotificationLevel(
        chatId: ChatId?
    )
}
