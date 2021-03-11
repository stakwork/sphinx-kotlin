package chat.sphinx.transactions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.transactions.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTransactionsDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.transactions_nav_graph,
            null,
            options
        )
    }
}
