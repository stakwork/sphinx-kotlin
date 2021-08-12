package chat.sphinx.tribe_members_list.navigation

import androidx.navigation.NavController
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class TribeMembersListNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun closeDetailScreen()
}
