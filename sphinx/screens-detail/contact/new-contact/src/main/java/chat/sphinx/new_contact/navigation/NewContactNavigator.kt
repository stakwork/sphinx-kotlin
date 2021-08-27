package chat.sphinx.new_contact.navigation

import androidx.navigation.NavController
import chat.sphinx.contact.navigation.ContactNavigator
import io.matthewnelson.concept_navigation.BaseNavigationDriver

abstract class NewContactNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): ContactNavigator(detailNavigationDriver)