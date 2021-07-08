package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.add_friend.navigation.AddFriendNavigator
import chat.sphinx.invite_friend.navigation.ToInviteFriendDetail
import chat.sphinx.new_contact.navigation.ToNewContactDetail
import javax.inject.Inject

internal class AddFriendNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): AddFriendNavigator(detailDriver) {

    override suspend fun toAddContactDetail() {
        navigationDriver.submitNavigationRequest(
            ToNewContactDetail()
        )
    }

    override suspend fun toInviteFriendDetail() {
        // TODO: Replace with actual
        navigationDriver.submitNavigationRequest(
            ToInviteFriendDetail()
        )
    }

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
