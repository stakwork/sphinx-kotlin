package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard_ready.navigation.OnBoardReadyNavigator
import javax.inject.Inject

internal class OnBoardReadyNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardReadyNavigator(navigationDriver)
{
    override suspend fun toDashboardScreen() {
        navigationDriver.submitNavigationRequest(
            ToDashboardScreen(
                popUpToId = R.id.main_primary_nav_graph,
                updateBackgroundLoginTime = true
            )
        )
    }
}
