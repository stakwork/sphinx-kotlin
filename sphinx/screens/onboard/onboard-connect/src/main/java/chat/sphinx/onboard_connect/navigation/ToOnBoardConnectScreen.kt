package chat.sphinx.onboard_connect.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard_connect.R
import chat.sphinx.onboard_connect.ui.OnBoardConnectFragmentArgs
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardConnectScreen(
    @IdRes private val popUpToId: Int,
    private val newUser: Boolean,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.on_board_connect_nav_graph,
            OnBoardConnectFragmentArgs.Builder(newUser).build().toBundle(),
            DefaultNavOptions.defaultAnims
                .setPopUpTo(popUpToId, false)
                .build()
        )
    }

}