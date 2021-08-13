package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.tribe_members_list.navigation.TribeMembersListNavigator
import javax.inject.Inject

internal class TribeMembersListNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): TribeMembersListNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
