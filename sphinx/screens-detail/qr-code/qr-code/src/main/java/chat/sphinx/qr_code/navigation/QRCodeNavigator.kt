package chat.sphinx.qr_code.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class QRCodeNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun closeDetailScreen()
}
