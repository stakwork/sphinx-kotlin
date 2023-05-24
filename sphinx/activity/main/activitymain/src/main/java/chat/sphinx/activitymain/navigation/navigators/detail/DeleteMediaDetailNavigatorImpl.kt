package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import chat.sphinx.example.delete_media_detail.navigation.DeleteMediaDetailNavigator
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import javax.inject.Inject

internal class DeleteMediaDetailNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): DeleteMediaDetailNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
