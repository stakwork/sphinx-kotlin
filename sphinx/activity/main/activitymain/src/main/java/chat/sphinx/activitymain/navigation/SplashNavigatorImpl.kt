package chat.sphinx.activitymain.navigation

import chat.sphinx.activitymain.R
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard.navigation.ToOnBoardView
import chat.sphinx.splash.navigation.SplashNavigator
import javax.inject.Inject

class SplashNavigatorImpl @Inject constructor(
    navigationDriver: MainNavigationDriver
): SplashNavigator(navigationDriver) {
    override suspend fun toScanner() {}

    override suspend fun toHomeScreen(privateMode: Boolean) {
        navigationDriver.submitNavigationRequest(
            ToDashboardScreen(R.id.main_nav_graph)
        )
    }

    override suspend fun toOnBoard(input: String) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardView(input)
        )
    }
}
