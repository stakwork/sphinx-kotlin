package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import chat.sphinx.episode_description.navigation.ToEpisodeDescription
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class VideoScreenNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {
    abstract suspend fun closeDetailScreen()

    abstract suspend fun toEpisodeDescriptionScreen(feedId: FeedId)
    abstract suspend fun toEpisodeDetail(
        feedItemId: FeedId?,
        header: String,
        image: String,
        episodeTypeImage: Int,
        episodeTypeText: String,
        episodeDate: String,
        episodeDuration: String,
        downloaded: Boolean?,
        link: FeedUrl?,
        )

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}


