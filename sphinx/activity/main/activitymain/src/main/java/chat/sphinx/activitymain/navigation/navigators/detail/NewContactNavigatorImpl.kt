package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.new_contact.navigation.NewContactNavigator
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import javax.inject.Inject

class NewContactNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver
): NewContactNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        navigationDriver.submitNavigationRequest(
            PopBackStack(destinationId = R.id.navigation_detail_blank_fragment, inclusive = false)
        )
    }
}
