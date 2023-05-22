package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import javax.inject.Inject

internal class ManageStorageNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): ManageStorageNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
