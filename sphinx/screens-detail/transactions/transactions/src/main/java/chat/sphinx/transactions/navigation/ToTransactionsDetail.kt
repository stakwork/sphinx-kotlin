package chat.sphinx.transactions.navigation

import androidx.navigation.NavController
import chat.sphinx.transactions.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTransactionsDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.transactions_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
