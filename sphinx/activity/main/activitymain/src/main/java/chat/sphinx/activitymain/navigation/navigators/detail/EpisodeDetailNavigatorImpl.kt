package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.episode_detail.navigation.EpisodeDetailNavigator
import chat.sphinx.transactions.navigation.TransactionsNavigator
import javax.inject.Inject

internal class EpisodeDetailNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): EpisodeDetailNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}