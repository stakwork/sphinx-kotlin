package chat.sphinx.payment_send.navigation

import androidx.navigation.NavController
import chat.sphinx.payment_common.navigation.PaymentNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class PaymentSendNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): PaymentNavigator(navigationDriver) {
    
    abstract suspend fun toPaymentTemplateDetail(
        contactId: ContactId?,
        chatId: ChatId?,
        amount: Sat,
        message: String
    )
}
