package chat.sphinx.tribe_badge.navigation

import androidx.navigation.NavController
import chat.sphinx.create_badge.navigation.ToCreateBadge
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class TribeBadgesNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun closeDetailScreen()

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    @JvmSynthetic
    suspend fun toCreateBadgeScreen(badgeName: String){
        navigationDriver.submitNavigationRequest(ToCreateBadge(badgeName))
    }

}
