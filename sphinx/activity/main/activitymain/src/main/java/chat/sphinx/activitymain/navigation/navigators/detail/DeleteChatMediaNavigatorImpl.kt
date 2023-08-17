package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.delete_chat_media.navigation.DeleteChatMediaNavigator
import chat.sphinx.delete_chat_media.navigation.ToDeleteChatMedia
import chat.sphinx.example.delete_chat_media_detail.navigation.ToDeleteChatMediaDetail
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import chat.sphinx.example.delete_media_detail.navigation.ToDeleteMediaDetail
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import chat.sphinx.tribe_badge.navigation.ToTribeBadges
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import javax.inject.Inject

internal class DeleteChatMediaNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
): DeleteChatMediaNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toDeleteChatDetail(chatId: ChatId) {
        detailDriver.submitNavigationRequest(ToDeleteChatMediaDetail(chatId))

    }
}
