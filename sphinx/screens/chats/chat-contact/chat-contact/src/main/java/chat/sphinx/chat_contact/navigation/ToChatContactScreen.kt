package chat.sphinx.chat_contact.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_contact.R
import chat.sphinx.chat_contact.ui.ChatContactFragmentArgs
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToChatContactScreen(
    private val chatId: ChatId?,
    private val contactId: ContactId,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.chat_contact_nav_graph,
            ChatContactFragmentArgs.Builder(
                contactId.value,
                chatId?.value ?: ChatId.NULL_CHAT_ID.toLong()
            ).build().toBundle(),
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
