package chat.sphinx.create_tribe.navigation

import androidx.navigation.NavController
import chat.sphinx.create_tribe.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToCreateTribeDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.create_tribe_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
