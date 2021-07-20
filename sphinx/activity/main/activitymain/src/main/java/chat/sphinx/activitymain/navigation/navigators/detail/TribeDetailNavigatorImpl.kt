package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import javax.inject.Inject

internal class TribeDetailNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): TribeDetailNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
