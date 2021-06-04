package chat.sphinx.dashboard.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.ui.DashboardFragmentArgs
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToDashboardScreen(
    @IdRes private val popUpToId: Int?,
    private val updateBackgroundLoginTime: Boolean = false
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val options: NavOptions = popUpToId?.let {
            DefaultNavOptions.defaultAnims
                .setPopUpTo(popUpToId, false)
                .build()
        } ?: DefaultNavOptions.defaultAnimsBuilt

        controller.navigate(
            R.id.dashboard_nav_graph,
            DashboardFragmentArgs.Builder(updateBackgroundLoginTime).build().toBundle(),
            options
        )
    }
}
