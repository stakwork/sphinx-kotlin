package chat.sphinx.discover_tribes.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class DiscoverTribesNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink)
}