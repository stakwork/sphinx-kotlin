package chat.sphinx.payment_template.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.payment_template.R
import chat.sphinx.payment_template.ui.PaymentTemplateFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPaymentTemplateDetail(
    private val contactId: ContactId?,
    private val chatId: ChatId?,
    private val amount: Sat,
    private val message: String,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.payment_template_nav_graph,

            PaymentTemplateFragmentArgs.Builder(
                contactId?.value ?: ContactId.NULL_CONTACT_ID.toLong(),
                chatId?.value ?: ChatId.NULL_CHAT_ID.toLong(),
                amount.value,
                message
            ).build().toBundle(),

            DetailNavOptions.default.apply {
                setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_left)
                setPopExitAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_out_right)
            }.build()
        )
    }
}