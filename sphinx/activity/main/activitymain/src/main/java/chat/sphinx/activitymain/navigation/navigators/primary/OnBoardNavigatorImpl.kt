package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard.navigation.OnBoardNavigator
import chat.sphinx.onboard_name.navigation.ToOnBoardNameScreen
import javax.inject.Inject

internal class OnBoardNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardNavigator(navigationDriver)
{
    override suspend fun toOnBoardNameScreen() {
        navigationDriver.submitNavigationRequest(
            ToOnBoardNameScreen(popUpToId = R.id.main_primary_nav_graph)
        )
    }
}
