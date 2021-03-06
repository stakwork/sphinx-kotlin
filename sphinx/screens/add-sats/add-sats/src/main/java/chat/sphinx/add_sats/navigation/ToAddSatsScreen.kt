package chat.sphinx.add_sats.navigation

import androidx.navigation.NavController
import chat.sphinx.add_sats.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.android_feature_navigation.R as R_navigation
import io.matthewnelson.concept_navigation.NavigationRequest

class ToAddSatsScreen: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.add_sats_nav_graph,
            null,
            DefaultNavOptions.defaultAnims
                .setEnterAnim(R_navigation.anim.slide_in_bottom)
                .setPopExitAnim(R_navigation.anim.slide_out_bottom)
                .build()
        )
    }
}
