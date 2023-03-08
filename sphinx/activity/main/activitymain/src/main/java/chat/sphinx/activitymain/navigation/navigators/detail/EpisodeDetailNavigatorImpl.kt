package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.episode_detail.navigation.EpisodeDetailNavigator
import chat.sphinx.transactions.navigation.TransactionsNavigator
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import javax.inject.Inject

internal class EpisodeDetailNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): EpisodeDetailNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun popBackStack() {
        (navigationDriver as DetailNavigationDriver).submitNavigationRequest(PopBackStack())
    }
}