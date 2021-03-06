package chat.sphinx.activitymain.navigation

import chat.sphinx.activitymain.R
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard.navigation.OnBoardNavigator
import javax.inject.Inject

class OnBoardNavigatorImpl @Inject constructor(
    navigationDriver: MainNavigationDriver
): OnBoardNavigator(navigationDriver)
{
    override suspend fun toDashboardScreen() {
        navigationDriver.submitNavigationRequest(
            ToDashboardScreen(R.id.main_nav_graph)
        )
    }
}
