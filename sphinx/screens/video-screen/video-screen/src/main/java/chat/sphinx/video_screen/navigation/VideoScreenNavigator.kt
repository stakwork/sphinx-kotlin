package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.feed.FeedId
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class VideoScreenNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver)
{
    abstract suspend fun toVideoFeedScreen(feedId: FeedId)

    abstract suspend fun toVideoWatchScreen(feedItemId: FeedId)
}
