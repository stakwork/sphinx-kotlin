package chat.sphinx.add_tribe_member.navigation

import androidx.navigation.NavController
import chat.sphinx.add_tribe_member.R
import chat.sphinx.add_tribe_member.ui.AddTribeMemberFragmentArgs
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToAddTribeMember(
    private val chatId: ChatId
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.add_tribe_member_nav_graph,
            AddTribeMemberFragmentArgs.Builder(chatId.value).build().toBundle(),
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
