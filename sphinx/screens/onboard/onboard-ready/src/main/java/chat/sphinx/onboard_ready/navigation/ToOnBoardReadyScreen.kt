package chat.sphinx.onboard_ready.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_ready.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardReadyScreen: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.on_board_ready_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
