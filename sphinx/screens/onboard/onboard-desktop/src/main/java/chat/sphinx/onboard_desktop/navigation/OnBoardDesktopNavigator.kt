package chat.sphinx.onboard_desktop.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardStep
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class OnBoardDesktopNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    abstract suspend fun toOnBoardReadyScreen(onBoardStep4: OnBoardStep.Step4_Ready)

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}