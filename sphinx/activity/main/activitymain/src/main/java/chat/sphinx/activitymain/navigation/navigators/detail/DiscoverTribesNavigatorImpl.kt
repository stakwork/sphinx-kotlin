package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.discover_tribes.navigation.DiscoverTribesNavigator
import javax.inject.Inject

internal class DiscoverTribesNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): DiscoverTribesNavigator(detailDriver) {

}
