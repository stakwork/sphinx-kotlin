package chat.sphinx.scanner.navigation

import androidx.navigation.NavController
import chat.sphinx.scanner.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToScannerDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.scanner_nav_graph,
            null,
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
