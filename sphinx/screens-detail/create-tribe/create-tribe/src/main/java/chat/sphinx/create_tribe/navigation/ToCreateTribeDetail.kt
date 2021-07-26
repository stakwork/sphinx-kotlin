package chat.sphinx.create_tribe.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.ui.CreateTribeFragmentArgs
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToCreateTribeDetail(
    private val chatId: ChatId? = null,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        controller.navigate(
            R.id.create_tribe_nav_graph,
            CreateTribeFragmentArgs
                .Builder(chatId?.value ?: ChatId.NULL_CHAT_ID.toLong())
                .build().toBundle(),
            options
        )
    }
}
