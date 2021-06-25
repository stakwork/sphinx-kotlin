package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import javax.inject.Inject

internal class QRCodeNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): QRCodeNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
