package chat.sphinx.join_tribe.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.join_tribe.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToJoinTribeDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.join_tribe_nav_graph,
            null,
            options
        )
    }
}
