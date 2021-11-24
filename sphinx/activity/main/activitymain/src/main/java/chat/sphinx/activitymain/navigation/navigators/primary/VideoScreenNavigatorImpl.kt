package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.video_screen.navigation.ToVideoFeedDetailScreen
import chat.sphinx.video_screen.navigation.ToVideoWatchScreen
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import chat.sphinx.wrapper_common.feed.FeedId
import javax.inject.Inject

internal class VideoScreenNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver
): VideoScreenNavigator(navigationDriver) {

    override suspend fun toVideoFeedScreen(feedId: FeedId) {
        navigationDriver.submitNavigationRequest(
            ToVideoFeedDetailScreen(
                feedId = feedId
            )
        )
    }

    override suspend fun toVideoWatchScreen(feedItemId: FeedId) {
        navigationDriver.submitNavigationRequest(
            ToVideoWatchScreen(
                feedItemId = feedItemId
            )
        )
    }

}
