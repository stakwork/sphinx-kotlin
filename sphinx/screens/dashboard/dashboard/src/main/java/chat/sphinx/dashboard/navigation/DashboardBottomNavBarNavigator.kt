package chat.sphinx.dashboard.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class DashboardBottomNavBarNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun toTransactionsDetail()
    abstract suspend fun toPaymentReceiveDetail()
    abstract suspend fun toPaymentSendDetail(lightningNodePubKey: LightningNodePubKey?, routeHint: LightningRouteHint?, contactId: ContactId?)
}
