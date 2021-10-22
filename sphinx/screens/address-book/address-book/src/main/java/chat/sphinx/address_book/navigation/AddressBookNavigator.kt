package chat.sphinx.address_book.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.dashboard.ContactId
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class AddressBookNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver)
{
    abstract suspend fun toAddFriendDetail()

    abstract suspend fun toEditContactDetail(contactId: ContactId)

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(
            PopBackStack()
        )
    }
}
