package chat.sphinx.onboard_description.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class OnBoardDescriptionNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    abstract suspend fun toOnBoardConnectScreen(newUser: Boolean)

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}