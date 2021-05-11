package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.add_friend.navigation.ToAddFriendDetail
import chat.sphinx.address_book.navigation.AddressBookNavigator
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import javax.inject.Inject

class AddressBookNavigatorImpl @Inject constructor(
    private val detailDriver: DetailNavigationDriver,
    navigationDriver: PrimaryNavigationDriver
): AddressBookNavigator(navigationDriver)
{
    override suspend fun toAddFriendDetail() {
        detailDriver.submitNavigationRequest(ToAddFriendDetail())
    }
}