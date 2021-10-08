package chat.sphinx.onboard_connected.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard_connected.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardConnectedScreen(
    @IdRes private val popUpToId: Int,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.on_board_connected_nav_graph,
            null,
            DefaultNavOptions.defaultAnims
                .setPopUpTo(popUpToId, false)
                .build()
        )
    }

}