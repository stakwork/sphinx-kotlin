package chat.sphinx.onboard_name.navigation

import androidx.navigation.NavController
import chat.sphinx.onboard_name.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardNameScreen: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.on_board_name_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
