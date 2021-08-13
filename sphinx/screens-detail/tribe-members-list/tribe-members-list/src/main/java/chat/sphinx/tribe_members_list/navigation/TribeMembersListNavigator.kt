package chat.sphinx.tribe_members_list.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class TribeMembersListNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun closeDetailScreen()

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}
