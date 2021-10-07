package chat.sphinx.onboard.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardStep
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class OnBoardMessageNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    abstract suspend fun toOnBoardLightning(onBoardStep2: OnBoardStep.Step2_Name)

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}
