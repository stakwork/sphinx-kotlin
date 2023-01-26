package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.tribes_discover.navigation.TribesDiscoverNavigator
import javax.inject.Inject

internal class TribesDiscoverNavigatorImpl @Inject constructor(
    private val detailDriver: DetailNavigationDriver,
): TribesDiscoverNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
