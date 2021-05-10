package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.new_contact.navigation.NewContactNavigator
import javax.inject.Inject

class NewContactNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver
): NewContactNavigator(detailDriver) {

}
