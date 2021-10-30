package chat.sphinx.camera.navigation

import androidx.navigation.NavController
import chat.sphinx.camera.R
import chat.sphinx.detail_resources.DetailNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

internal class ToCameraDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        try {
            controller.getBackStackEntry(R.id.navigation_camera_fragment)
            return
        } catch (e: IllegalArgumentException) {}
        controller.navigate(
            R.id.camera_nav_graph,
            null,
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
