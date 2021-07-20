package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.contact_detail.navigation.ToContactDetailScreen
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.podcast_player.navigation.ToPodcastPlayerScreen
import chat.sphinx.podcast_player.objects.ParcelablePodcast
import chat.sphinx.tribe_detail.navigation.ToTribeDetailScreen
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import javax.inject.Inject

internal class TribeChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
): TribeChatNavigator(navigationDriver)
{

    override suspend fun toPaymentSendDetail(contactId: ContactId, chatId: ChatId?) {
        detailDriver.submitNavigationRequest(ToPaymentSendDetail(contactId, chatId))
    }

    override suspend fun toPodcastPlayerScreen(chatId: ChatId, podcast: ParcelablePodcast) {
        detailDriver.submitNavigationRequest(ToPodcastPlayerScreen(chatId, podcast))
    }

    override suspend fun toChatDetail(chatId: ChatId, contactId: ContactId?) {
        detailDriver.submitNavigationRequest(ToContactDetailScreen(chatId, contactId))
    }

    override suspend fun toTribeDetailScreen(chatId: ChatId, podcast: Podcast?) {
        detailDriver.submitNavigationRequest(ToTribeDetailScreen(chatId, podcast))
    }
}
