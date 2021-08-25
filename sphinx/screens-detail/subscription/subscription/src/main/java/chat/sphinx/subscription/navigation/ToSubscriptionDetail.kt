package chat.sphinx.subscription.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.subscription.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToSubscriptionDetail(): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.subscription_nav_graph,
            null,
            DetailNavOptions.default.apply {
                setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_left)
                setPopExitAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_out_right)
            }.build()
        )
    }
}
