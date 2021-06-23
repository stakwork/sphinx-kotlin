package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import javax.inject.Inject

internal class ContactChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
): ContactChatNavigator(navigationDriver)
{
    override suspend fun toPaymentSendDetail(contactId: ContactId, chatId: ChatId?) {
        detailDriver.submitNavigationRequest(ToPaymentSendDetail(contactId, chatId))
    }
}
