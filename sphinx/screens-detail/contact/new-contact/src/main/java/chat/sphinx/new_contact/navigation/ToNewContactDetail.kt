package chat.sphinx.new_contact.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.new_contact.R
import chat.sphinx.new_contact.ui.NewContactFragmentArgs
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToNewContactDetail(
    private val pubKey: LightningNodePubKey? = null,
    private val routeHint: LightningRouteHint? = null,
    private val fromAddFriendView: Boolean = true
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        val args = NewContactFragmentArgs.Builder(fromAddFriendView)
        args.argPubKey = pubKey?.value
        args.argRouteHint = routeHint?.value

        val navOptions = if (fromAddFriendView) {
            DetailNavOptions.default.apply {
                setEnterAnim(nav_R.anim.slide_in_left)
                setPopExitAnim(nav_R.anim.slide_out_right)
            }.build()
        } else {
            DetailNavOptions.defaultBuilt
        }

        controller.navigate(
            R.id.new_contact_nav_graph,
            args.build().toBundle(),
            navOptions
        )
    }
}
