package chat.sphinx.onboard_connecting.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardStep
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class OnBoardConnectingNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    abstract suspend fun toOnBoardConnectedScreen()
    abstract suspend fun toOnBoardNameScreen()

    abstract suspend fun toDashboardScreen()

    abstract suspend fun toOnBoardMessageScreen(onBoardStep1: OnBoardStep.Step1_WelcomeMessage)

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}
