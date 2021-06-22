package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_group.navigation.GroupChatNavigator
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.send_attachment.navigation.ToSendAttachmentDetail
import chat.sphinx.wrapper_common.dashboard.ContactId
import javax.inject.Inject

internal class GroupChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
): GroupChatNavigator(navigationDriver)
{
    override suspend fun toPaymentSendDetail(contactId: ContactId) {
        detailDriver.submitNavigationRequest(ToPaymentSendDetail(contactId))
    }

}
