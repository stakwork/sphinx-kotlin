package chat.sphinx.create_tribe.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.ui.CreateTribeFragmentArgs
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToEditTribeDetail(
    private val chatId: ChatId,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        controller.navigate(
            R.id.create_tribe_nav_graph,
            CreateTribeFragmentArgs
                .Builder(chatId.value)
                .build().toBundle(),
            if (controller.previousBackStackEntry == null) {
                DetailNavOptions.defaultBuilt
            } else {
                DetailNavOptions.default
                    .setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_left)
                    .setPopExitAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_out_right)
                    .build()
            }
        )
    }
}
