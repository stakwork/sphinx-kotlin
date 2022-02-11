package chat.sphinx.scanner.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class ScannerNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {

    @JvmSynthetic
    internal suspend fun toScannerScreen(
        showBottomView: Boolean = false,
        scannerModeLabel: String = ""
    ) {
        navigationDriver.submitNavigationRequest(ToScannerDetail(showBottomView, scannerModeLabel))
    }

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun closeDetailScreen()
}
