package chat.sphinx.onboard.navigation

import android.os.Bundle
import androidx.navigation.NavController
import chat.sphinx.onboard.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardView(val input: String): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.on_board_nav_graph,
            Bundle().apply { putString(USER_INPUT, input) },
            DefaultNavOptions.defaultAnimsBuilt
        )
    }

    companion object {
        const val USER_INPUT = "USER_INPUT"
    }
}
