package chat.sphinx.podcast_player.navigation

import androidx.navigation.NavController
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
        currentEpisodeDuration: Long,
    ) {
        navigationDriver.submitNavigationRequest(ToPodcastPlayerScreen(chatId, feedId, feedUrl, currentEpisodeDuration, false))
    }

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    abstract suspend fun closeDetailScreen()
}
