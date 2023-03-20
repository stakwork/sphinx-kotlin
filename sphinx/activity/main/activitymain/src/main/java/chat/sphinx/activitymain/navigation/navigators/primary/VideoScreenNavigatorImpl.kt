package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import javax.inject.Inject

internal class VideoScreenNavigatorImpl @Inject constructor(
    navigationDriver: DetailNavigationDriver,
): VideoScreenNavigator(navigationDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

}
