package chat.sphinx.common_player.navigation

import androidx.navigation.NavController
import chat.sphinx.episode_description.navigation.ToEpisodeDescription
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class CommonPlayerNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
    @JvmSynthetic
    internal suspend fun toEpisodeDescriptionScreen(
        feedId: FeedId,
    ) {
        navigationDriver.submitNavigationRequest(ToEpisodeDescription(feedId, true))
    }
    abstract suspend fun toEpisodeDetail(
        feedItemId: FeedId?,
        header: String,
        image: String,
        episodeTypeImage: Int,
        episodeTypeText: String,
        episodeDate: String,
        episodeDuration: String,
        downloaded: Boolean?,
        link: FeedUrl?
    )

    abstract suspend fun closeDetailScreen()
}
