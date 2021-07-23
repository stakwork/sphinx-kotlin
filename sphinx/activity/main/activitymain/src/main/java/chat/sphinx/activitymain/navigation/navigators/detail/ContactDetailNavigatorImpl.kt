package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.contact_detail.navigation.ContactDetailNavigator
import javax.inject.Inject

internal class ContactDetailNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): ContactDetailNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
