package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.onboard.navigation.ToOnBoardScreen
import chat.sphinx.onboard_common.model.OnBoardStep
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

    override suspend fun toOnBoardScreen(onBoardStep1: OnBoardStep.Step1) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep = onBoardStep1)
        )
    }
}
