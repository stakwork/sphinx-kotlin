package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_lightning.navigation.OnBoardLightningNavigator
import chat.sphinx.onboard_lightning.navigation.ToOnBoardLightningScreen
import javax.inject.Inject

internal class OnBoardLightningNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardLightningNavigator(navigationDriver) {

    override suspend fun toOnBoardNameScreen(onBoardStep2: OnBoardStep.Step2_Name) {
        navigationDriver.submitNavigationRequest(
            ToOnBoardLightningScreen(popUpToId = R.id.main_primary_nav_graph, onBoardStep2)
        )
    }

}