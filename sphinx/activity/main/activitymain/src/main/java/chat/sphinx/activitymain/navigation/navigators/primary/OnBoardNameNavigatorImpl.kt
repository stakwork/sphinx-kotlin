package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_name.navigation.OnBoardNameNavigator
import chat.sphinx.onboard_ready.navigation.ToOnBoardReadyScreen
import javax.inject.Inject

internal class OnBoardNameNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardNameNavigator(navigationDriver)
{
    override suspend fun toOnBoardReadyScreen(onBoardStep3: OnBoardStep.Step3) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardReadyScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep3)
        )
    }
}
