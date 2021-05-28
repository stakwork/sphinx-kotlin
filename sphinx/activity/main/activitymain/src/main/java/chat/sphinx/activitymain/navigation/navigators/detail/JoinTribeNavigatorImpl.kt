package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import javax.inject.Inject

internal class JoinTribeNavigatorImpl  @Inject constructor (
    detailDriver: DetailNavigationDriver,
): JoinTribeNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}