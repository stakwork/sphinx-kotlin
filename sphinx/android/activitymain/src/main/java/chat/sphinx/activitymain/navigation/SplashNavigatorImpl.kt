package chat.sphinx.activitymain.navigation

import chat.sphinx.splash.navigation.SplashNavigator
import javax.inject.Inject

class SplashNavigatorImpl @Inject constructor(
    navigationDriver: MainNavigationDriver
): SplashNavigator(navigationDriver) {

}