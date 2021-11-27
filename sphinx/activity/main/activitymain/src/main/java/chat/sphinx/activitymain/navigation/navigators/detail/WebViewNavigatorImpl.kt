package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.add_friend.navigation.AddFriendNavigator
import chat.sphinx.invite_friend.navigation.ToInviteFriendDetail
import chat.sphinx.new_contact.navigation.ToNewContactDetail
import chat.sphinx.newsletter_detail.navigation.NewsletterDetailNavigator
import chat.sphinx.web_view.navigation.WebViewNavigator
import javax.inject.Inject

internal class WebViewNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): WebViewNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
