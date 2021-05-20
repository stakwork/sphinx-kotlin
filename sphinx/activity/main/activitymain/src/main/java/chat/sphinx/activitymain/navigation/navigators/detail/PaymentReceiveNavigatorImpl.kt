package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
import javax.inject.Inject

internal class PaymentReceiveNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): PaymentReceiveNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
