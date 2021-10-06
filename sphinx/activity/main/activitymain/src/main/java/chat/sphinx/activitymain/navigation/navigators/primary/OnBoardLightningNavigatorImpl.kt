package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_lightning.navigation.OnBoardLightningNavigator
import javax.inject.Inject

internal class OnBoardLightningNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardLightningNavigator(navigationDriver) {



}