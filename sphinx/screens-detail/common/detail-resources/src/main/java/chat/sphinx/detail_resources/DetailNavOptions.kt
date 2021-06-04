package chat.sphinx.detail_resources

import androidx.navigation.NavOptions
import io.matthewnelson.android_feature_navigation.R as R_navigation

object DetailNavOptions {

    @Suppress("SpellCheckingInspection")
    val default: NavOptions.Builder
        get() = NavOptions.Builder()
            .setLaunchSingleTop(true)
//            .setEnterAnim(R_navigation.anim.slide_in_bottom)
            .setExitAnim(R_navigation.anim.slide_out_left)
            .setPopEnterAnim(R_navigation.anim.slide_in_right)
            .setPopExitAnim(R_navigation.anim.slide_out_bottom)

    @Suppress("SpellCheckingInspection")
    val defaultBuilt: NavOptions by lazy {
        default.build()
    }
}