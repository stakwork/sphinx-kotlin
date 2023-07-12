package chat.sphinx.dashboard.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class DashboardNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {

    abstract suspend fun toChatContact(chatId: ChatId?, contactId: ContactId)
    abstract suspend fun toChatGroup(chatId: ChatId)
    abstract suspend fun toChatTribe(chatId: ChatId)
    abstract suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink)
    abstract suspend fun toQRCodeDetail(qrText: String, viewTitle: String, description: String? = null)

    abstract suspend fun toAddContactDetail(
        pubKey: LightningNodePubKey,
        routeHint: LightningRouteHint? = null
    )

    abstract suspend fun toVideoWatchScreen(chatId: ChatId, feedId: FeedId, feedUrl: FeedUrl)

    abstract suspend fun toWebViewDetail(
        chatId: ChatId?,
        title: String,
        url: FeedUrl,
        feedId: FeedId?,
        feedItemId: FeedId?
    )

    abstract suspend fun toNewsletterDetail(chatId: ChatId, feedUrl: FeedUrl)

    abstract suspend fun toPodcastPlayerScreen(
        chatId: ChatId,
        feedId: FeedId,
        feedUrl: FeedUrl,
        fromDownloadedSection: Boolean = false
    )

    abstract suspend fun toCommonPlayerScreen(
        podcastId: FeedId,
        episodeId: FeedId
    )

}
