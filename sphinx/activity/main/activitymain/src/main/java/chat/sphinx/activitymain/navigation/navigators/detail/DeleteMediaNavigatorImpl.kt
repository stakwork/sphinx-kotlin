package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import chat.sphinx.example.delete_media_detail.navigation.ToDeleteMediaDetail
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import chat.sphinx.tribe_badge.navigation.ToTribeBadges
import chat.sphinx.wrapper_common.feed.FeedId
import javax.inject.Inject

internal class DeleteMediaNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
): DeleteMediaNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toDeleteMediaDetail(feedId: FeedId) {
        detailDriver.submitNavigationRequest(ToDeleteMediaDetail(feedId))
    }
}
