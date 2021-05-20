package chat.sphinx.chat_tribe.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.ui.ChatTribeFragmentArgs
import chat.sphinx.wrapper_common.chat.ChatId
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToChatTribeScreen(
    private val chatId: ChatId
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.chat_tribe_nav_graph,
            ChatTribeFragmentArgs.Builder(chatId.value).build().toBundle(),
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
