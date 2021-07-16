package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
import javax.inject.Inject

internal class PaymentReceiveNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
): PaymentReceiveNavigator(navigationDriver) {
    override suspend fun closeDetailScreen() {
        detailDriver.closeDetailScreen()
    }

    override suspend fun toQRCodeDetail(qrText: String, viewTitle: String, description: String, showBackButton: Boolean) {
        detailDriver.submitNavigationRequest(
            ToQRCodeDetail(
                qrText,
                viewTitle,
                description,
                showBackButton
            )
        )
    }
}
