package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import javax.inject.Inject

internal class CommonPlayerNavigatorImpl @Inject constructor (
    detailDriver: DetailNavigationDriver,
): CommonPlayerNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

}
