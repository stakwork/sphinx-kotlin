package chat.sphinx.payment_common.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.payment_common.R
import chat.sphinx.payment_common.ui.PaymentFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPaymentSendDetail(
    private val contactId: ContactId? = null,
    private val chatId: ChatId? = null,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {

    companion object {
        const val NULL_CONTACT_ID = Long.MAX_VALUE
    }

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.payment_send_nav_graph,
            PaymentFragmentArgs.Builder(
                contactId?.value ?: NULL_CONTACT_ID,
                chatId?.value ?: ChatId.NULL_CHAT_ID.toLong()
            ).build().toBundle(),
            options
        )
    }
}
