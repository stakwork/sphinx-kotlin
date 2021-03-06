package chat.sphinx.payment_send.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.payment_send.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPaymentSendDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.payment_send_nav_graph,
            null,
            options
        )
    }
}
