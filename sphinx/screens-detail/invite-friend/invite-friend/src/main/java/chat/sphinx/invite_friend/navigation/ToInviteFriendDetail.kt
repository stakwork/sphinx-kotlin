package chat.sphinx.invite_friend.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.invite_friend.R
import io.matthewnelson.android_feature_navigation.R as nav_R
import io.matthewnelson.concept_navigation.NavigationRequest

class ToInviteFriendDetail: NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.invite_friend_nav_graph,
            null,
            DetailNavOptions.default.apply {
                setEnterAnim(nav_R.anim.slide_in_left)
                setPopExitAnim(nav_R.anim.slide_out_right)
            }.build()
        )
    }
}
