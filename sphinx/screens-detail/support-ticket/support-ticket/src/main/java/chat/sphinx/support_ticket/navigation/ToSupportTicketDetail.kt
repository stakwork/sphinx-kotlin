package chat.sphinx.support_ticket.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.support_ticket.R
import io.matthewnelson.android_feature_navigation.R as R_navigation
import io.matthewnelson.concept_navigation.NavigationRequest

class ToSupportTicketDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.support_ticket_nav_graph,
            null,
            options
        )
    }
}
