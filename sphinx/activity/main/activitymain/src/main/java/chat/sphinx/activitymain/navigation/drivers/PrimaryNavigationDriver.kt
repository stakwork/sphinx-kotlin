package chat.sphinx.activitymain.navigation.drivers

import androidx.navigation.NavController
import chat.sphinx.activitymain.R
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.feature_navigation.NavigationDriver

internal class PrimaryNavigationDriver: NavigationDriver<NavController>(replayCacheSize = 5) {
    override suspend fun whenTrueExecuteRequest(request: NavigationRequest<NavController>): Boolean {
        return true
    }

    suspend fun toDashboardScreen(deepLink: String) {
        submitNavigationRequest(
            ToDashboardScreen(
                popUpToId = R.id.main_primary_nav_graph,
                updateBackgroundLoginTime = false,
                deepLink
            )
        )
    }
}
