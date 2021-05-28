package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.dashboard.navigation.DashboardBottomNavBarNavigator
import chat.sphinx.join_tribe.navigation.ToJoinTribeDetail
import chat.sphinx.payment_receive.navigation.ToPaymentReceiveDetail
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.transactions.navigation.ToTransactionsDetail
import chat.sphinx.wrapper_common.TribeJoinLink
import javax.inject.Inject

internal class DashboardBottomNavBarNavigatorImpl @Inject constructor(
    navigationDriver: DetailNavigationDriver
): DashboardBottomNavBarNavigator(navigationDriver)
{
    override suspend fun toTransactionsDetail() {
        navigationDriver.submitNavigationRequest(ToTransactionsDetail())
    }

    override suspend fun toPaymentReceiveDetail() {
        navigationDriver.submitNavigationRequest(ToPaymentReceiveDetail())
    }

    override suspend fun toPaymentSendDetail() {
        navigationDriver.submitNavigationRequest(ToPaymentSendDetail())
    }

    override suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink) {
        navigationDriver.submitNavigationRequest(ToJoinTribeDetail(tribeLink))
    }
}
