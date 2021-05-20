package chat.sphinx.chat_group.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_group.R
import chat.sphinx.chat_group.ui.ChatGroupFragmentArgs
import chat.sphinx.wrapper_chat.Chat
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToChatGroupScreen(
    val chat: Chat
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.chat_group_nav_graph,
            ChatGroupFragmentArgs.Builder(chat.id.value).build().toBundle(),
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
