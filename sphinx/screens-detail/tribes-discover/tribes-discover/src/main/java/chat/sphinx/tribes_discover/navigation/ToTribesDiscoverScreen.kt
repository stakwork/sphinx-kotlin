package chat.sphinx.tribes_discover.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.tribes_discover.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTribesDiscoverScreen(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.discover_tribes_nav_graph,
            null,
            options
        )
    }
}