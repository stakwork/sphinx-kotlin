package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.edit_contact.navigation.EditContactNavigator
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
import chat.sphinx.subscription.navigation.ToSubscriptionDetail
import chat.sphinx.wrapper_common.dashboard.ContactId
import javax.inject.Inject

internal class EditContactNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): EditContactNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toQRCodeDetail(qrText: String, viewTitle: String, description: String) {
        navigationDriver.submitNavigationRequest(
            ToQRCodeDetail(
                qrText,
                viewTitle,
                description,
                true
            )
        )
    }

    override suspend fun toSubscribeDetailScreen(contactId: ContactId) {
        navigationDriver.submitNavigationRequest(
            ToSubscriptionDetail(contactId)
        )
    }
}
