package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.add_tribe_member.navigation.AddTribeMemberNavigator
import javax.inject.Inject

internal class AddTribeMemberNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): AddTribeMemberNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
