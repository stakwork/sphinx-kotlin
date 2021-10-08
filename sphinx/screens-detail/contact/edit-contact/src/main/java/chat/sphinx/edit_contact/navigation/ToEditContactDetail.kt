package chat.sphinx.edit_contact.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.edit_contact.R
import chat.sphinx.edit_contact.ui.EditContactFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ContactId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToEditContactDetail(
    private val contactId: ContactId,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        val args = EditContactFragmentArgs.Builder(contactId.value)

        controller.navigate(
            R.id.edit_contact_nav_graph,
            args.build().toBundle(),
            DetailNavOptions.defaultBuilt
        )
    }
}
