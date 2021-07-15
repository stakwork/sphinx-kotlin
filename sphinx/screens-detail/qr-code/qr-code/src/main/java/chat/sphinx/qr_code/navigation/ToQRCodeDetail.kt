package chat.sphinx.qr_code.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.qr_code.R
import chat.sphinx.qr_code.ui.QRCodeFragmentArgs
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToQRCodeDetail(
    private val qrText: String,
    private val viewTitle: String,
    private val description: String? = null,
    private val showBackArrow: Boolean? = null
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val builder = QRCodeFragmentArgs.Builder(
            qrText,
            viewTitle,
            description ?: "",
            showBackArrow ?: (controller.previousBackStackEntry != null)
        )

        controller.navigate(
            R.id.qr_code_nav_graph,
            builder.build().toBundle(),
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
