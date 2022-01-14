package chat.sphinx.newsletter_detail.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class NewsletterDetailNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {

    abstract suspend fun closeDetailScreen()

    abstract suspend fun toWebViewDetail(
        chatId: ChatId?,
        title: String,
        url: FeedUrl,
        feedId: FeedId?,
        feedItemId: FeedId?
    )

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }
}