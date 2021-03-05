package chat.sphinx.dashboard.navigation

import androidx.navigation.NavController
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class BottomNavBarNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun toReceivePaymentDetail()
    abstract suspend fun toTransactionHistoryDetail()
    abstract suspend fun toScannerDetail()
    abstract suspend fun toSendPaymentDetail()
}