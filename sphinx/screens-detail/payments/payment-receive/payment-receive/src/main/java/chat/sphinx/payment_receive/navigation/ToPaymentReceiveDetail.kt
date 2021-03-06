package chat.sphinx.payment_receive.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.payment_receive.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPaymentReceiveDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.payment_receive_nav_graph,
            null,
            options
        )
    }
}
