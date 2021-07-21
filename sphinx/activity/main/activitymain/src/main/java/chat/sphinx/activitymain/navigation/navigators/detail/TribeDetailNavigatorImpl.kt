package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import javax.inject.Inject

internal class TribeDetailNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
): TribeDetailNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        detailDriver.closeDetailScreen()
    }

    override suspend fun toShareTribeScreen(
        qrText: String,
        viewTitle: String,
        description: String?,
    ) {
        detailDriver.submitNavigationRequest(ToQRCodeDetail(qrText, viewTitle, description))
    }
}
