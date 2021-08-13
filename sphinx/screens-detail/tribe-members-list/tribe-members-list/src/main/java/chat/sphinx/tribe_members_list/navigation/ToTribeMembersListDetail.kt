package chat.sphinx.tribe_members_list.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.tribe_members_list.R
import chat.sphinx.tribe_members_list.ui.TribeMembersListFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTribeMembersListDetail(
    private val chatId: ChatId
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.tribe_members_list_nav_graph,
            TribeMembersListFragmentArgs.Builder(chatId.value).build().toBundle(),
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
