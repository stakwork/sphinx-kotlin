package chat.sphinx.activitymain.navigation.drivers

import androidx.navigation.NavController
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.feature_navigation.NavigationDriver

class PrimaryNavigationDriver: NavigationDriver<NavController>(replayCacheSize = 5) {
    override suspend fun whenTrueExecuteRequest(request: NavigationRequest<NavController>): Boolean {
        return true
    }
}
