package chat.sphinx.add_sats.navigation

import androidx.navigation.NavController
import chat.sphinx.add_sats.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToAddSatsScreen: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.add_sats_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
