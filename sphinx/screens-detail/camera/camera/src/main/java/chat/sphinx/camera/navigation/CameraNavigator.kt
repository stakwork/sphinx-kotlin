package chat.sphinx.camera.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class CameraNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    @JvmSynthetic
    internal suspend fun toCameraDetailScreen(
        replacingVideoFragment: Boolean = false
    ) {
        navigationDriver.submitNavigationRequest(
            ToCameraDetail(
                replacingVideoFragment = replacingVideoFragment
            )
        )
    }

    @JvmSynthetic
    internal suspend fun toCaptureVideoDetailScreen() {
        navigationDriver.submitNavigationRequest(ToCaptureVideoDetail())
    }

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun closeDetailScreen()
}
