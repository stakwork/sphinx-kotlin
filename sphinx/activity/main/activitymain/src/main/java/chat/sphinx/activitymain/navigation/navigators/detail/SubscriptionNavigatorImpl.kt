package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.subscription.navigation.SubscriptionNavigator
import javax.inject.Inject

internal class SubscriptionNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): SubscriptionNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
