package chat.sphinx.onboard_picture.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardStep
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class OnBoardPictureNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun toOnBoardDesktopScreen(onBoardStep4: OnBoardStep.Step4_Ready)
}
