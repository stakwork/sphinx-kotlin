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
        newUser: Boolean,
        restoreKeys: String?,
        connectionCode: String?,
    ) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardConnectingScreen(
                popUpToId = R.id.main_primary_nav_graph,
                newUser = newUser,
                restoreKeys = restoreKeys,
                connectionCode = connectionCode
            )
        )
    }
}