package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.send_attachment.navigation.SendAttachmentNavigator
import javax.inject.Inject

internal class SendAttachmentNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): SendAttachmentNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}