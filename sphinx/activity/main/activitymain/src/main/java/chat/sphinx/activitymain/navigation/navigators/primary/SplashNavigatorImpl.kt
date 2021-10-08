package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard.navigation.ToOnBoardScreen
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_name.navigation.ToOnBoardNameScreen
import chat.sphinx.onboard_picture.navigation.ToOnBoardPictureScreen
import chat.sphinx.onboard_ready.navigation.ToOnBoardReadyScreen
import chat.sphinx.onboard_welcome.navigation.ToOnBoardWelcomeScreen
import chat.sphinx.splash.navigation.SplashNavigator
import javax.inject.Inject

internal class SplashNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): SplashNavigator(navigationDriver) {

    override suspend fun toDashboardScreen(
        privateMode: Boolean,
        updateBackgroundLoginTime: Boolean,
    ) {
        navigationDriver.submitNavigationRequest(
            ToDashboardScreen(
                popUpToId = R.id.main_primary_nav_graph,
                updateBackgroundLoginTime = updateBackgroundLoginTime,
            )
        )
    }

    override suspend fun toOnBoardWelcomeScreen() {
        navigationDriver.submitNavigationRequest(
            ToOnBoardWelcomeScreen(popUpToId = R.id.main_primary_nav_graph)
        )
    }

    override suspend fun toOnBoardMessageScreen(onBoardStep1: OnBoardStep.Step1_WelcomeMessage) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep = onBoardStep1)
        )
    }

    override suspend fun toOnBoardNameScreen(onBoardStep2: OnBoardStep.Step2_Name) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardNameScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep = onBoardStep2)
        )
    }

    override suspend fun toOnBoardPictureScreen(onBoardStep3: OnBoardStep.Step3_Picture) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardPictureScreen(
                popUpToId = R.id.main_primary_nav_graph,
                onBoardStep = onBoardStep3,
                refreshContacts = true,
            )
        )
    }

    override suspend fun toOnBoardReadyScreen(onBoardStep4: OnBoardStep.Step4_Ready) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardReadyScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep = onBoardStep4)
        )
    }
}
