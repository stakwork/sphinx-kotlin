package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.profile.navigation.ProfileNavigator
import javax.inject.Inject

class ProfileNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): ProfileNavigator(navigationDriver)