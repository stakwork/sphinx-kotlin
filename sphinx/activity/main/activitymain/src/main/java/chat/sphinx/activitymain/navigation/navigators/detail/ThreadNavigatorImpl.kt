package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.delete_chat_media.navigation.ToDeleteChatMedia
import chat.sphinx.example.delete_media.navigation.ToDeletePodcast
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.threads.navigation.ThreadNavigator
import javax.inject.Inject

internal class ThreadNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
): ThreadNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

}
