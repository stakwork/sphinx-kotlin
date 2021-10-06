package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard_connected.navigation.OnBoardConnectedNavigator
import javax.inject.Inject

internal class OnBoardConnectedNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardConnectedNavigator(navigationDriver) {

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

}