package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.podcast_player.navigation.ToPodcastPlayerScreen
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.send_attachment.navigation.ToSendAttachmentDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import javax.inject.Inject

internal class TribeChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
    ): TribeChatNavigator(navigationDriver)
{

    override suspend fun toPaymentSendDetail(contactId: ContactId) {
        detailDriver.submitNavigationRequest(ToPaymentSendDetail(contactId))
    }

    override suspend fun toPodcastPlayerScreen(chatId: ChatId, podcast: Podcast) {
        detailDriver.submitNavigationRequest(ToPodcastPlayerScreen(chatId, podcast))
    }
}
