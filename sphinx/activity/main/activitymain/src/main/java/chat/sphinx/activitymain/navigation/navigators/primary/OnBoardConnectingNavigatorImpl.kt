package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard.navigation.ToOnBoardScreen
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_connect.navigation.OnBoardConnectNavigator
import chat.sphinx.onboard_connected.navigation.ToOnBoardConnectedScreen
import chat.sphinx.onboard_connecting.navigation.OnBoardConnectingNavigator
import chat.sphinx.onboard_desktop.navigation.ToOnBoardDesktopScreen
import chat.sphinx.onboard_lightning.navigation.ToOnBoardLightningScreen
import chat.sphinx.onboard_name.navigation.ToOnBoardNameScreen
import javax.inject.Inject

internal class OnBoardConnectingNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardConnectingNavigator(navigationDriver) {

    override suspend fun toOnBoardConnectedScreen() {
        navigationDriver.submitNavigationRequest(
            ToOnBoardConnectedScreen(
                popUpToId = R.id.main_primary_nav_graph
            )
        )
    }

    override suspend fun toOnBoardNameScreen() {
        navigationDriver.submitNavigationRequest(
            ToOnBoardNameScreen(popUpToId = R.id.main_primary_nav_graph, null)
        )
    }

    override suspend fun toDashboardScreen() {
        navigationDriver.submitNavigationRequest(
            ToDashboardScreen(
                popUpToId = R.id.main_primary_nav_graph,
                updateBackgroundLoginTime = true
            )
        )
    }

    override suspend fun toOnBoardMessageScreen(onBoardStep1: OnBoardStep.Step1_WelcomeMessage) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep = onBoardStep1)
        )
    }

}