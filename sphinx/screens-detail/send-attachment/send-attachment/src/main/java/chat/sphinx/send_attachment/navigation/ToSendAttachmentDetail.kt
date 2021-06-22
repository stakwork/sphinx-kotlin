package chat.sphinx.send_attachment.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.send_attachment.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToSendAttachmentDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.send_attachment_nav_graph,
            null,
            options
        )
    }
}