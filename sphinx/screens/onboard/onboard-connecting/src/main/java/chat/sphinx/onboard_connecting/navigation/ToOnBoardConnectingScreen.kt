package chat.sphinx.onboard_connecting.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard_connecting.R
import chat.sphinx.onboard_connecting.ui.OnBoardConnectingFragmentArgs
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardConnectingScreen(
    @IdRes private val popUpToId: Int,
    private val newUser: Boolean,
    private val restoreKeys: String? = null,
    private val connectionCode: String? = null,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {

        OnBoardConnectingFragmentArgs.Builder(
            newUser,
            restoreKeys,
            connectionCode
        ).build().toBundle()?.let { args ->

            controller.navigate(
                R.id.on_board_connecting_nav_graph,
                args,
                DefaultNavOptions.defaultAnims
                    .setPopUpTo(popUpToId, false)
                    .build()
            )

        }
    }

}