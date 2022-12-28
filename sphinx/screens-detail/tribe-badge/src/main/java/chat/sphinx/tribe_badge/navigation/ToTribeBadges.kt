package chat.sphinx.tribe_badge.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.tribe_badge.R
import chat.sphinx.tribe_badge.ui.TribeBadgesFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTribeBadges(
    private val chatId: ChatId,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        controller.navigate(
            R.id.tribe_badges_nav_graph,
            TribeBadgesFragmentArgs
                .Builder(chatId.value)
                .build().toBundle(),
            options

        )
    }
}
