package chat.sphinx.add_friend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.add_friend.R
import chat.sphinx.detail_resources.DetailNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToAddFriendDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.add_friend_nav_graph,
            null,
            options
        )
    }
}
