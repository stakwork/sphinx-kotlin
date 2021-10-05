package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_welcome.navigation.OnBoardWelcomeNavigator
import javax.inject.Inject

internal class OnBoardWelcomeNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardWelcomeNavigator(navigationDriver) {
}