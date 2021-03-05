package chat.sphinx.home.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.home.R
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToHomeScreen(@IdRes private val popUpToId: Int?): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val options: NavOptions = popUpToId?.let {
            DefaultNavOptions.defaultAnims
                .setPopUpTo(popUpToId, false)
                .build()
        } ?: DefaultNavOptions.defaultAnimsBuilt

        controller.navigate(
            R.id.home_nav_graph,
            null,
            options
        )
    }
}