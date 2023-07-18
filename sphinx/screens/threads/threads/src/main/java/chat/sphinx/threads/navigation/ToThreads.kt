package chat.sphinx.threads.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.threads.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToThreads(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.threads_nav_graph,
            null,
            options
        )
    }

}
