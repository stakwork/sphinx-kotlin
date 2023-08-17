package chat.sphinx.known_badges.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.known_badges.R
import chat.sphinx.known_badges.ui.KnownBadgesFragmentArgs
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToKnownBadges(
    private val badgeIds: Array<String>,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        val builder = KnownBadgesFragmentArgs.Builder(badgeIds)

        controller.navigate(
            R.id.known_badges_nav_graph,
            builder.build().toBundle(),
            if (controller.previousBackStackEntry == null) {
                DetailNavOptions.defaultBuilt
            } else {
                DetailNavOptions.default
                    .setEnterAnim(nav_R.anim.slide_in_left)
                    .setPopExitAnim(nav_R.anim.slide_out_right)
                    .build()
            }
        )
    }
}
