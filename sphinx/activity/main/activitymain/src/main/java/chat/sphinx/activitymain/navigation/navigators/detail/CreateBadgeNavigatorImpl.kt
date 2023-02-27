package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.create_badge.navigation.CreateBadgeNavigator
import javax.inject.Inject

internal class CreateBadgeNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): CreateBadgeNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
