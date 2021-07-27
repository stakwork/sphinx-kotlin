package chat.sphinx.splash.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardStep
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class SplashNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver)
{
    abstract suspend fun toDashboardScreen(
        privateMode: Boolean = false,
        updateBackgroundLoginTime: Boolean = false,
    )

    abstract suspend fun toOnBoardScreen(onBoardStep1: OnBoardStep.Step1)
    abstract suspend fun toOnBoardNameScreen(onBoardStep2: OnBoardStep.Step2)
    abstract suspend fun toOnBoardPictureScreen(onBoardStep3: OnBoardStep.Step3)
    abstract suspend fun toOnBoardReadyScreen(onBoardStep4: OnBoardStep.Step4)
}
