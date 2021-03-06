package chat.sphinx.payment_receive.navigation

import androidx.navigation.NavController
import chat.sphinx.payment_receive.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPaymentReceiveDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.payment_receive_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
