package chat.sphinx.tribes_discover.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class TribesDiscoverNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {

    @JvmSynthetic
    internal suspend fun toTribesDiscover() {
        navigationDriver.submitNavigationRequest(ToTribesDiscoverScreen())
    }

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun closeDetailScreen()
}