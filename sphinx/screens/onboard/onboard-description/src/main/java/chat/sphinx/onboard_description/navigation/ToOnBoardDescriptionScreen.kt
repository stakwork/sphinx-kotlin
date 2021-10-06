package chat.sphinx.onboard_description.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard_description.R
import chat.sphinx.onboard_description.ui.OnBoardDescriptionFragmentArgs
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToOnBoardDescriptionScreen(
    @IdRes private val popUpToId: Int,
    private val newUser: Boolean,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.on_board_description_nav_graph,
            OnBoardDescriptionFragmentArgs.Builder(newUser).build().toBundle(),
            DefaultNavOptions.defaultAnims
                .setPopUpTo(popUpToId, false)
                .build()
        )
    }

}