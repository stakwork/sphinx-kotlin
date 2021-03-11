package chat.sphinx.chat_group.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_group.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToChatGroupScreen: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.chat_group_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
