package chat.sphinx.edit_contact.navigation

import androidx.navigation.NavController
import chat.sphinx.contact.navigation.ContactNavigator
import io.matthewnelson.concept_navigation.BaseNavigationDriver

abstract class EditContactNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): ContactNavigator(detailNavigationDriver)