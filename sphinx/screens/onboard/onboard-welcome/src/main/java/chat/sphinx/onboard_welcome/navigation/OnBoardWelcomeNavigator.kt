package chat.sphinx.onboard_welcome.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardStep
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class OnBoardWelcomeNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    abstract suspend fun toOnBoardDescriptionScreen(newUser: Boolean)

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}
