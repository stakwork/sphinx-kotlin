package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import javax.inject.Inject

internal class PodcastPlayerNavigatorImpl  @Inject constructor (
    detailDriver: DetailNavigationDriver,
): PodcastPlayerNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
