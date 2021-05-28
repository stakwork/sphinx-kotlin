package chat.sphinx.join_tribe.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.join_tribe.R
import chat.sphinx.join_tribe.ui.JoinTribeFragmentArgs
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.concept_navigation.NavigationRequest

class ToJoinTribeDetail(
    private val tribeLink: TribeJoinLink,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.join_tribe_nav_graph,
            JoinTribeFragmentArgs.Builder(tribeLink.value).build().toBundle(),
            options
        )
    }
}
