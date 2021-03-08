package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard.navigation.OnBoardNavigator
import javax.inject.Inject

class OnBoardNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardNavigator(navigationDriver)
{
    override suspend fun toDashboardScreen() {
        navigationDriver.submitNavigationRequest(
            ToDashboardScreen(R.id.main_primary_nav_graph)
        )
    }
}
