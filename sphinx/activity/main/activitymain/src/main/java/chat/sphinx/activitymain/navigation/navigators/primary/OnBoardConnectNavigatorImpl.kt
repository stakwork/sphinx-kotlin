package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_connect.navigation.OnBoardConnectNavigator
import chat.sphinx.onboard_connect.navigation.ToOnBoardConnectScreen
import chat.sphinx.onboard_connecting.navigation.ToOnBoardConnectingScreen
import javax.inject.Inject

internal class OnBoardConnectNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardConnectNavigator(navigationDriver) {

    override suspend fun toOnBoardConnectingScreen(
        code: String?,
    ) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardConnectingScreen(
                code = code,
            )
        )
    }
}