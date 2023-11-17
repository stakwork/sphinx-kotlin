package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_name.navigation.OnBoardNameNavigator
import chat.sphinx.onboard_picture.navigation.ToOnBoardPictureScreen
import javax.inject.Inject

internal class OnBoardNameNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardNameNavigator(navigationDriver)
{
    override suspend fun toOnBoardPictureScreen(onBoardStep3: OnBoardStep.Step3_Picture?) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardPictureScreen(
                popUpToId = R.id.main_primary_nav_graph,
                onBoardStep = onBoardStep3,
                refreshContacts = false,
            )
        )
    }
}
