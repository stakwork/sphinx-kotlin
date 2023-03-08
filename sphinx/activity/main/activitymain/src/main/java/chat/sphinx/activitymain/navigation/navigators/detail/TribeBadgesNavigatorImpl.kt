package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.tribe_badge.navigation.TribeBadgesNavigator
import javax.inject.Inject

internal class TribeBadgesNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): TribeBadgesNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
