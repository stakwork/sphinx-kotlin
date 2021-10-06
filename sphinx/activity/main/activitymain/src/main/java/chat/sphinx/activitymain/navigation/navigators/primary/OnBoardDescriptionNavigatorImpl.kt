package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_description.navigation.OnBoardDescriptionNavigator
import javax.inject.Inject

internal class OnBoardDescriptionNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardDescriptionNavigator(navigationDriver) {


}