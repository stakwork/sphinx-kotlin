package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import javax.inject.Inject

internal class PaymentSendNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): PaymentSendNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
