package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.transactions.navigation.TransactionsNavigator
import javax.inject.Inject

internal class TransactionsNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): TransactionsNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
