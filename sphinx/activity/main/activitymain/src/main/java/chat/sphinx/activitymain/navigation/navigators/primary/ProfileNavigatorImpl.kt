package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.example.manage_storage.navigation.ToManageStorageDetail
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
import chat.sphinx.profile.navigation.ProfileNavigator
import javax.inject.Inject

internal class ProfileNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    val detailDriver: DetailNavigationDriver,
): ProfileNavigator(navigationDriver) {
    override suspend fun toQRCodeDetail(qrText: String, viewTitle: String) {}

    override suspend fun toManageStorageDetail() {
        detailDriver.submitNavigationRequest(ToManageStorageDetail())
    }
}