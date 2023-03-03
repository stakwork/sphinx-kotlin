package chat.sphinx.create_badge.navigation

import androidx.navigation.NavController
import chat.sphinx.create_badge.R
import chat.sphinx.create_badge.ui.CreateBadgeFragmentArgs
import chat.sphinx.detail_resources.DetailNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToCreateBadge(
    private val name: String,
    private val description: String,
    private val image: String,
    private val rewardType: Int,
    private val rewardRequirement: Int,
    private val isActive: Boolean,
    private val chatId: Int,
    private val badgeId: Int,
    private val amountCreated: Int,
    private val amountIssued: Int,
    private val claimAmount: Int,
    private val holderType: Int
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        controller.navigate(
            R.id.create_badge_nav_graph,
            CreateBadgeFragmentArgs
                .Builder(
                    name,
                    description,
                    image,
                    rewardType,
                    rewardRequirement,
                    isActive,
                    chatId,
                    badgeId,
                    amountCreated,
                    amountIssued,
                    claimAmount,
                    holderType
                )
                .build().toBundle(),
            DetailNavOptions.default
                .setEnterAnim(nav_R.anim.slide_in_left)
                .setPopExitAnim(nav_R.anim.slide_out_right)
                .build()
        )
    }

}
