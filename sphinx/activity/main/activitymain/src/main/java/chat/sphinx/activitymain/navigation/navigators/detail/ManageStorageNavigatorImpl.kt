package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.example.delete_media.navigation.ToDeletePodcast
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import javax.inject.Inject

internal class ManageStorageNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
): ManageStorageNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toDeleteMediaDetail() {
        detailDriver.submitNavigationRequest(ToDeletePodcast())
    }
}
