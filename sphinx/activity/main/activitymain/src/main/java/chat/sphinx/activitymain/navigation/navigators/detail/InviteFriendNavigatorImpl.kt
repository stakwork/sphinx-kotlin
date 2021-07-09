package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.invite_friend.navigation.InviteFriendNavigator
import javax.inject.Inject

internal class InviteFriendNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): InviteFriendNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}