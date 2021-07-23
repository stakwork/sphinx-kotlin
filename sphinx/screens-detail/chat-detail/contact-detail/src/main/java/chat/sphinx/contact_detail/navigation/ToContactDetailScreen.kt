package chat.sphinx.contact_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.contact_detail.R
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToContactDetailScreen(
    val chatId: ChatId,
    val contactId: ContactId?,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.contact_detail_nav_graph,
            null,
            options
        )
    }
}
