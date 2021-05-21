package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard.navigation.ToOnBoardScreen
import chat.sphinx.splash.navigation.SplashNavigator
import javax.inject.Inject

internal class SplashNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): SplashNavigator(navigationDriver) {
    override suspend fun toDashboardScreen(privateMode: Boolean) {
        navigationDriver.submitNavigationRequest(
            ToDashboardScreen(R.id.main_primary_nav_graph)
        )
    }

    override suspend fun toOnBoardScreen(input: String) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardScreen(input)
        )
    }
}
