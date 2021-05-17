package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.add_friend.navigation.AddFriendNavigator
import chat.sphinx.new_contact.navigation.ToNewContactDetail
import chat.sphinx.support_ticket.navigation.ToSupportTicketDetail
import chat.sphinx.transactions.navigation.ToTransactionsDetail
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import javax.inject.Inject

class AddFriendNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver
): AddFriendNavigator(detailDriver) {
    override suspend fun toAddContactDetail() {
        navigationDriver.submitNavigationRequest(
            ToNewContactDetail()
        )
    }

    override suspend fun toCreateInvitationDetail() {
        // TODO: Replace with actual
        navigationDriver.submitNavigationRequest(
            ToTransactionsDetail(
                DefaultNavOptions.defaultAnims
                    .setLaunchSingleTop(true)
                    .build()
            )
        )
    }

    override suspend fun closeDetailScreen() {
        navigationDriver.submitNavigationRequest(
            PopBackStack(destinationId = R.id.navigation_detail_blank_fragment, inclusive = false)
        )
    }
}
