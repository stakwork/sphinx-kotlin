package chat.sphinx.edit_contact.navigation

import androidx.navigation.NavController
import chat.sphinx.contact.navigation.ContactNavigator
import chat.sphinx.wrapper_common.dashboard.ContactId
import io.matthewnelson.concept_navigation.BaseNavigationDriver

abstract class EditContactNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): ContactNavigator(detailNavigationDriver) {
    abstract suspend fun toSubscribeDetailScreen(contactId: ContactId)
}