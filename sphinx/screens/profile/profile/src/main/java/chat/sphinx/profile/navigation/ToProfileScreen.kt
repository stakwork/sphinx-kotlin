package chat.sphinx.profile.navigation

import androidx.navigation.NavController
import chat.sphinx.profile.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToProfileScreen: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.profile_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
