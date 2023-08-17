package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.known_badges.navigation.KnownBadgesNavigator
import chat.sphinx.newsletter_detail.navigation.NewsletterDetailNavigator
import chat.sphinx.notification_level.navigation.NotificationLevelNavigator
import chat.sphinx.notification_level.navigation.ToNotificationLevel
import chat.sphinx.web_view.navigation.ToWebViewDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import javax.inject.Inject

internal class KnownBadgesNavigatorImpl @Inject constructor(
    private val detailDriver: DetailNavigationDriver,
): KnownBadgesNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        detailDriver.closeDetailScreen()
    }
}
