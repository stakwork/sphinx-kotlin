package chat.sphinx.tribe_badge.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.tribe_badge.R
import chat.sphinx.tribe_badge.ui.TribeBadgesFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToTribeBadges(
    private val chatId: ChatId,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        controller.navigate(
            R.id.tribe_badges_nav_graph,
            TribeBadgesFragmentArgs
                .Builder(chatId.value)
                .build().toBundle(),
            DetailNavOptions.default
                .setEnterAnim(nav_R.anim.slide_in_left)
                .setPopExitAnim(nav_R.anim.slide_out_right)
                .build()
        )
    }

}
