package chat.sphinx.tribe_members_list.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.tribe_members_list.R
import chat.sphinx.tribe_members_list.ui.TribeMembersListFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTribeMembersListDetail(
    private val chatId: ChatId,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.tribe_members_list_nav_graph,
            TribeMembersListFragmentArgs.Builder(chatId.value).build().toBundle(),
            options
        )
    }
}
