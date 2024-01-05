package chat.sphinx.activitymain.navigation.drivers

import androidx.navigation.NavController
import chat.sphinx.activitymain.R
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.feature_navigation.NavigationDriver

class DetailNavigationDriver: NavigationDriver<NavController>(replayCacheSize = 2) {
    override suspend fun whenTrueExecuteRequest(request: NavigationRequest<NavController>): Boolean {
        return true
    }

    suspend fun closeDetailScreen() {
        submitNavigationRequest(
            PopBackStack(destinationId = R.id.navigation_detail_blank_fragment, inclusive = false)
        )
    }
}
