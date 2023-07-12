package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_contact.navigation.ToChatContactScreen
import chat.sphinx.chat_group.navigation.ToChatGroupScreen
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.common_player.navigation.ToCommonPlayerScreen
import chat.sphinx.dashboard.navigation.DashboardNavigator
import chat.sphinx.join_tribe.navigation.ToJoinTribeDetail
import chat.sphinx.new_contact.navigation.ToNewContactDetail
import chat.sphinx.newsletter_detail.navigation.ToNewsletterDetailScreen
import chat.sphinx.podcast_player.navigation.ToPodcastPlayerScreen
import chat.sphinx.qr_code.navigation.ToQRCodeDetail
import chat.sphinx.video_screen.navigation.ToVideoWatchScreen
import chat.sphinx.web_view.navigation.ToWebViewDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import javax.inject.Inject

internal class DashboardNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
): DashboardNavigator(navigationDriver)
{
    override suspend fun toChatContact(chatId: ChatId?, contactId: ContactId) {
        navigationDriver.submitNavigationRequest(
            ToChatContactScreen(chatId, contactId)
        )
    }

    override suspend fun toChatGroup(chatId: ChatId) {
        navigationDriver.submitNavigationRequest(
            ToChatGroupScreen(chatId)
        )
    }

    override suspend fun toChatTribe(chatId: ChatId) {
        navigationDriver.submitNavigationRequest(
            ToChatTribeScreen(chatId)
        )
    }

    override suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink) {
        detailDriver.submitNavigationRequest(ToJoinTribeDetail(tribeLink))
    }

    override suspend fun toQRCodeDetail(qrText: String, viewTitle: String, description: String?) {
        detailDriver.submitNavigationRequest(
            ToQRCodeDetail(
                qrText,
                viewTitle,
                description
            )
        )
    }

    override suspend fun toAddContactDetail(pubKey: LightningNodePubKey, routeHint: LightningRouteHint?) {
        detailDriver.submitNavigationRequest(
            ToNewContactDetail(pubKey, routeHint, false)
        )
    }

    override suspend fun toWebViewDetail(
        chatId: ChatId?,
        title: String,
        url: FeedUrl,
        feedId: FeedId?,
        feedItemId: FeedId?
    ) {
        detailDriver.submitNavigationRequest(
            ToWebViewDetail(chatId, title, url, feedId, feedItemId, false)
        )
    }

    override suspend fun toNewsletterDetail(chatId: ChatId, feedUrl: FeedUrl) {
        detailDriver.submitNavigationRequest(
            ToNewsletterDetailScreen(chatId, feedUrl)
        )
    }

    override suspend fun toPodcastPlayerScreen(
        chatId: ChatId,
        feedId: FeedId,
        feedUrl: FeedUrl,
        fromDownloadedSection: Boolean
    ) {
        detailDriver.submitNavigationRequest(ToPodcastPlayerScreen(chatId, feedId, feedUrl, true, fromDownloadedSection))
    }

    override suspend fun toCommonPlayerScreen(podcastId: FeedId, episodeId: FeedId) {
        detailDriver.submitNavigationRequest(
            ToCommonPlayerScreen(podcastId, episodeId)
        )
    }

    override suspend fun toVideoWatchScreen(chatId: ChatId, feedId: FeedId, feedUrl: FeedUrl) {
        detailDriver.submitNavigationRequest(
            ToVideoWatchScreen(
                chatId, feedId, feedUrl
            )
        )
    }
}
