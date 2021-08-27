package chat.sphinx.tribe_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.ui.TribeDetailFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTribeDetailScreen(
    val chatId: ChatId,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        val args = TribeDetailFragmentArgs.Builder(chatId.value)

        controller.navigate(
            R.id.tribe_detail_nav_graph,
            args.build().toBundle(),
            options
        )
    }
}
