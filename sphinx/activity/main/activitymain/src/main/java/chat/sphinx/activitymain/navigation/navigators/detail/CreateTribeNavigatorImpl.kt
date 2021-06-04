package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.create_tribe.navigation.CreateTribeNavigator
import javax.inject.Inject

internal class CreateTribeNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): CreateTribeNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }
}
