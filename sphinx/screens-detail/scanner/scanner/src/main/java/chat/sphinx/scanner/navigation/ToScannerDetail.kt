package chat.sphinx.scanner.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.scanner.R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToScannerDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.scanner_nav_graph,
            null,
            options
        )
    }
}
