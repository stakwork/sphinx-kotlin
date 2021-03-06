package chat.sphinx.dashboard.navigation

import androidx.navigation.NavController
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class NavDrawerNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun toAddSatsScreen()
    abstract suspend fun toContactsScreen()
    abstract suspend fun toProfileScreen()
    abstract suspend fun toAddFriendDetail()
    abstract suspend fun toCreateTribeDetail()
    abstract suspend fun toSupportTicketDetail()
    abstract suspend fun logout()
}