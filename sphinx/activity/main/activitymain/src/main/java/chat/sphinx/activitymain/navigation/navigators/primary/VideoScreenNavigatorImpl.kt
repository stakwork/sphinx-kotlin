package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.episode_detail.navigation.ToEpisodeDetail
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import javax.inject.Inject

internal class VideoScreenNavigatorImpl @Inject constructor(
    navigationDriver: DetailNavigationDriver,
): VideoScreenNavigator(navigationDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toEpisodeDetail() {
        (navigationDriver as DetailNavigationDriver).submitNavigationRequest(ToEpisodeDetail())
    }
}
