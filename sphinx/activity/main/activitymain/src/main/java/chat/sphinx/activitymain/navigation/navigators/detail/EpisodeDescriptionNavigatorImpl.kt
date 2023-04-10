package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import javax.inject.Inject

internal class EpisodeDescriptionNavigatorImpl@Inject constructor(
    detailDriver: DetailNavigationDriver,
): EpisodeDescriptionNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun popBackStack() {
        (navigationDriver as DetailNavigationDriver).submitNavigationRequest(PopBackStack())
    }
}