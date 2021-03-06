package chat.sphinx.payment_send.navigation

import androidx.navigation.NavController
import chat.sphinx.payment_send.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPaymentSendDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.payment_send_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
