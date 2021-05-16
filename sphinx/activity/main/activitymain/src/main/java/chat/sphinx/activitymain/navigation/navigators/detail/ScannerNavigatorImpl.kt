package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.scanner.navigation.ScannerNavigator
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import javax.inject.Inject

@ActivityRetainedScoped
class ScannerNavigatorImpl @Inject constructor(
    navigationDriver: DetailNavigationDriver,
): ScannerNavigator(navigationDriver) {
    override suspend fun closeDetailScreen() {
        navigationDriver.submitNavigationRequest(
            PopBackStack(destinationId = R.id.navigation_detail_blank_fragment, inclusive = false)
        )
    }
}
