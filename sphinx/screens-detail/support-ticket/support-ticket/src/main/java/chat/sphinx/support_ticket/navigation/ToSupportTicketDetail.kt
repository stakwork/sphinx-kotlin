package chat.sphinx.support_ticket.navigation

import androidx.navigation.NavController
import chat.sphinx.support_ticket.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToSupportTicketDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.support_ticket_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
