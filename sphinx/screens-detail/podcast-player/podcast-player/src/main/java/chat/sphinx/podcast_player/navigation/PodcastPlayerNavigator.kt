package chat.sphinx.podcast_player.navigation

import androidx.navigation.NavController
import chat.sphinx.episode_description.navigation.ToEpisodeDescription
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class PodcastPlayerNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {

    @JvmSynthetic
    internal suspend fun toPodcastPlayerScreen(
        chatId: ChatId,
        feedId: FeedId,
        feedUrl: FeedUrl,
        fromDownloadedSection: Boolean = false
    ) {
        navigationDriver.submitNavigationRequest(ToPodcastPlayerScreen(chatId, feedId, feedUrl, false, fromDownloadedSection))
    }

    @JvmSynthetic
    internal suspend fun toEpisodeDescriptionScreen(
        feedId: FeedId,
    ) {
        navigationDriver.submitNavigationRequest(ToEpisodeDescription(feedId, false))
    }

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun closeDetailScreen()
}
