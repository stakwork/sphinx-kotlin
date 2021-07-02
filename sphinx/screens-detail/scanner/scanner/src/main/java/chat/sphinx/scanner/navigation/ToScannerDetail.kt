package chat.sphinx.scanner.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.scanner.R
import chat.sphinx.scanner.ui.ScannerFragmentArgs
import io.matthewnelson.android_feature_navigation.R as nav_R
import io.matthewnelson.concept_navigation.NavigationRequest

internal class ToScannerDetail(
    private val showBottomView: Boolean = false,
    private val scannerModeLabel: String = "",
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        try {
            // Only navigate to the scanner detail screen if it is
            // _not_ on the backstack, as doing so will break the
            // ScannerCoordinator's shared flow connection.
            controller.getBackStackEntry(R.id.navigation_scanner_fragment)
            return
        } catch (e: IllegalArgumentException) {}

        controller.navigate(
            R.id.scanner_nav_graph,

            ScannerFragmentArgs.Builder(controller.previousBackStackEntry != null, showBottomView, scannerModeLabel)
                .build()
                .toBundle(),

            if (controller.previousBackStackEntry == null) {
                DetailNavOptions.defaultBuilt
            } else {
                DetailNavOptions.default
                    .setEnterAnim(nav_R.anim.slide_in_left)
                    .setPopExitAnim(nav_R.anim.slide_out_right)
                    .build()
            }
        )
    }
}
