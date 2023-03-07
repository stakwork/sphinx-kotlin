package chat.sphinx.episode_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.episode_detail.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToEpisodeDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.episode_detail_nav_graph,
            null,
            options
        )
    }
}
