package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.payment_receive.navigation.ToPaymentReceiveDetail
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.scanner.navigation.ToScannerDetail
import chat.sphinx.transactions.navigation.ToTransactionsDetail
import javax.inject.Inject

class DashboardBottomNavBarNavigatorImpl @Inject constructor(
    navigationDriver: DetailNavigationDriver
): DashboardBottomNavBarNavigator(navigationDriver)
{
    override suspend fun toScannerDetail() {
        navigationDriver.submitNavigationRequest(ToScannerDetail())
    }

    override suspend fun toTransactionsDetail() {
        navigationDriver.submitNavigationRequest(ToTransactionsDetail())
    }

    override suspend fun toPaymentReceiveDetail() {
        navigationDriver.submitNavigationRequest(ToPaymentReceiveDetail())
    }

    override suspend fun toPaymentSendDetail() {
        navigationDriver.submitNavigationRequest(ToPaymentSendDetail())
    }
}
