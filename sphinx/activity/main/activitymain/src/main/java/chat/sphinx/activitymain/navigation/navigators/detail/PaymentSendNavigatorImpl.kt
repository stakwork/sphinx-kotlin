package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.new_contact.navigation.ToNewContactDetail
import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import chat.sphinx.payment_template.navigation.ToPaymentTemplateDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import javax.inject.Inject

internal class PaymentSendNavigatorImpl @Inject constructor(
    detailDriver: DetailNavigationDriver,
): PaymentSendNavigator(detailDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toPaymentTemplateDetail(
        contactId: ContactId?,
        chatId: ChatId?,
        amount: Sat,
        message: String
    ) {
        navigationDriver.submitNavigationRequest(
            ToPaymentTemplateDetail(contactId, chatId, amount, message)
        )
    }
}
