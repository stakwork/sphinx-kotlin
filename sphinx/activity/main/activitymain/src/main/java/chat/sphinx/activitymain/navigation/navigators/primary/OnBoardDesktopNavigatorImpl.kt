package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_desktop.navigation.OnBoardDesktopNavigator
import chat.sphinx.onboard_lightning.navigation.OnBoardLightningNavigator
import javax.inject.Inject

internal class OnBoardDesktopNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardDesktopNavigator(navigationDriver) {


}