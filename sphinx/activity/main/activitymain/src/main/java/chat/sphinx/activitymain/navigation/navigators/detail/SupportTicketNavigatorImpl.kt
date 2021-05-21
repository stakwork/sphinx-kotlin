package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import javax.inject.Inject

internal class SupportTicketNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): SupportTicketNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
