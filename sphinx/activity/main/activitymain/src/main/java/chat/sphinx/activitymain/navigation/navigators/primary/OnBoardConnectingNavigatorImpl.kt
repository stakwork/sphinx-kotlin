package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_connect.navigation.OnBoardConnectNavigator
import chat.sphinx.onboard_connecting.navigation.OnBoardConnectingNavigator
import javax.inject.Inject

internal class OnBoardConnectingNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): OnBoardConnectingNavigator(navigationDriver) {

    override suspend fun toOnBoardConnectedScreen() {
        //TODO("Not yet implemented")
    }

    override suspend fun toOnBoardMessageScreen(onBoardStep1Message: OnBoardStep.Step1_WelcomeMessage) {
        //TODO("Not yet implemented")
    }

}