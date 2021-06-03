package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.podcast_player.navigation.TribeChatPodcastPlayerNavigator
import javax.inject.Inject

internal class TribeChatPodcastPlayerNavigatorImpl  @Inject constructor (
    detailDriver: DetailNavigationDriver,
): TribeChatPodcastPlayerNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
