package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.newsletter_detail.navigation.NewsletterDetailNavigator
import chat.sphinx.web_view.navigation.ToWebViewDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import javax.inject.Inject

internal class NewsletterDetailNavigatorImpl @Inject constructor(
    private val detailDriver: DetailNavigationDriver,
): NewsletterDetailNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toWebViewDetail(
        chatId: ChatId?,
        title: String,
        url: FeedUrl,
        feedId: FeedId?,
        feedItemId: FeedId?
    ) {
        detailDriver.submitNavigationRequest(
            ToWebViewDetail(chatId, title, url, feedId, feedItemId, true)
        )
    }
}
