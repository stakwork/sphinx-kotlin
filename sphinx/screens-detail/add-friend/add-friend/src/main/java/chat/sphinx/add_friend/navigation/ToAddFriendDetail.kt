package chat.sphinx.add_friend.navigation

import androidx.navigation.NavController
import chat.sphinx.add_friend.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToAddFriendDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.add_friend_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
