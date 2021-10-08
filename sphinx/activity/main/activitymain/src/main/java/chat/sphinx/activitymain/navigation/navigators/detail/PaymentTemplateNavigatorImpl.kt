package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.payment_template.navigation.PaymentTemplateNavigator
import javax.inject.Inject

internal class PaymentTemplateNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): PaymentTemplateNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}