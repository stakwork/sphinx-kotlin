package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import javax.inject.Inject

internal class VideoScreenNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): VideoScreenNavigator(navigationDriver) {


}
