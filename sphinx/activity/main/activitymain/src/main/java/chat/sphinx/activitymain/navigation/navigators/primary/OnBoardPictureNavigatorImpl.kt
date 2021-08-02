package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_picture.navigation.OnBoardPictureNavigator
import chat.sphinx.onboard_ready.navigation.ToOnBoardReadyScreen
import javax.inject.Inject

internal class OnBoardPictureNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardPictureNavigator(navigationDriver)
{
    override suspend fun toOnBoardReadyScreen(onBoardStep4: OnBoardStep.Step4_Ready) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardReadyScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep4)
        )
    }
}
