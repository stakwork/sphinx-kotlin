package chat.sphinx.common_player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.common_player.R
import chat.sphinx.common_player.ui.CommonPlayerScreenFragmentArgs
import chat.sphinx.detail_resources.DetailNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToCommonPlayerScreen(
    private val recommendations: List<String>,
    private val recommendationId: String,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
) : NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.common_player_nav_graph,
            CommonPlayerScreenFragmentArgs.Builder(
                recommendations.toTypedArray(),
                recommendationId
            ).build().toBundle(),
            options
        )
    }
}