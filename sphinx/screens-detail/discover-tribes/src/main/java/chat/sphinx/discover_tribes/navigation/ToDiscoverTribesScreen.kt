package chat.sphinx.discover_tribes.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.discover_tribes.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToDiscoverTribesScreen(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.discover_tribes_nav_graph,
            null,
            DetailNavOptions.default
                .setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_left)
                .setPopExitAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_out_right)
                .build()
        )
    }
}