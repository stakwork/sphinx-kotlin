package chat.sphinx.activitymain.navigation

import chat.sphinx.onboard.navigation.OnBoardNavigator
import javax.inject.Inject

class OnBoardNavigatorImpl @Inject constructor(
    navigationDriver: MainNavigationDriver
): OnBoardNavigator(navigationDriver) {
    override suspend fun toHomeScreen() {
        // TODO("Not yet implemented")
    }
}
