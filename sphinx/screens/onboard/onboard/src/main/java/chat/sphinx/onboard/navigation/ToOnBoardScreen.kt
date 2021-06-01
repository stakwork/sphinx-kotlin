package chat.sphinx.onboard.navigation

import android.os.Bundle
import androidx.navigation.NavController
import chat.sphinx.onboard.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardScreen(): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.on_board_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }

    companion object {
        const val USER_INPUT = "USER_INPUT"
    }
}
