package chat.sphinx.payment_send.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.ui.PaymentSendFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.message.MessageUUID
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPaymentSendDetail(
    private val contactId: ContactId? = null,
    private val chatId: ChatId? = null,
    private val messageUUID: MessageUUID? = null,
    private val lightningNodePubKey: LightningNodePubKey? = null,
    private val routeHint: LightningRouteHint? = null,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.payment_send_nav_graph,
            PaymentSendFragmentArgs.Builder(
                contactId?.value ?: ContactId.NULL_CONTACT_ID,
                chatId?.value ?: ChatId.NULL_CHAT_ID.toLong(),
                messageUUID?.value ?: "",
                lightningNodePubKey?.value ?: "",
                routeHint?.value ?: ""
            ).build().toBundle(),
            options
        )
    }
}
