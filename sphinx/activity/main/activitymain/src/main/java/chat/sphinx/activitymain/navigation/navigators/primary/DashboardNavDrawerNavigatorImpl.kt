package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.add_friend.navigation.ToAddFriendDetail
import chat.sphinx.add_sats.navigation.ToAddSatsScreen
import chat.sphinx.address_book.navigation.ToAddressBookScreen
import chat.sphinx.create_tribe.navigation.ToCreateTribeDetail
import chat.sphinx.dashboard.navigation.DashboardNavDrawerNavigator
import chat.sphinx.profile.navigation.ToProfileScreen
import chat.sphinx.support_ticket.navigation.ToSupportTicketDetail
import javax.inject.Inject

internal class DashboardNavDrawerNavigatorImpl @Inject constructor(
    private val detailDriver: DetailNavigationDriver,
    primaryDriver: PrimaryNavigationDriver
): DashboardNavDrawerNavigator(primaryDriver)
{
    override suspend fun toAddSatsScreen() {
        // TODO: Should this be loaded as a detail screen as to not disrupt the dashboard?
        navigationDriver.submitNavigationRequest(ToAddSatsScreen())
    }

    override suspend fun toAddressBookScreen() {
        navigationDriver.submitNavigationRequest(ToAddressBookScreen())
    }

    override suspend fun toProfileScreen() {
        navigationDriver.submitNavigationRequest(ToProfileScreen())
    }

    override suspend fun toAddFriendDetail() {
        detailDriver.submitNavigationRequest(ToAddFriendDetail())
    }

    override suspend fun toCreateTribeDetail() {
        detailDriver.submitNavigationRequest(ToCreateTribeDetail())
    }

    override suspend fun toSupportTicketDetail() {
        detailDriver.submitNavigationRequest(ToSupportTicketDetail())
    }

    override suspend fun logout() {
        // TODO: Add API to AuthenticationCoordinator and AuthenticationManager
        //  to log out then send to splash screen.
        //  Think about "accounts" and how that will need to work regarding use
        //  of different PINs w/o storing a hash of the pin... (problematic)
        //  .
        //  IDEA: Maybe store a salted hash of the PIN for non-primary account
        //  which would allow for lookup? If the PIN hash doesn't exist, try to decrypt
        //  primary account information?
    }
}
