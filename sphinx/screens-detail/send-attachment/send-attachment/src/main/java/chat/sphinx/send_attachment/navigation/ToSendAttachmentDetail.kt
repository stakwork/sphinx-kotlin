package chat.sphinx.send_attachment.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.send_attachment.R
import io.matthewnelson.concept_navigation.NavigationRequest
import chat.sphinx.send_attachment.ui.SendAttachmentFragmentArgs

class ToSendAttachmentDetail(
    private val isConversation: Boolean = false
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.send_attachment_nav_graph,

            SendAttachmentFragmentArgs.Builder(isConversation)
                .build()
                .toBundle(),

            DetailNavOptions.defaultBuilt
        )
    }
}
