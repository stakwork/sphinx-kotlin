package chat.sphinx.notification_level.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.notification_level.R
import chat.sphinx.notification_level.ui.NotificationLevelFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToNotificationLevel(
    private val chatId: ChatId?,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.notification_level_nav_graph,
            NotificationLevelFragmentArgs.Builder(
                chatId?.value ?: ChatId.NULL_CHAT_ID.toLong()
            ).build().toBundle(),
            if (controller.previousBackStackEntry == null) {
                DetailNavOptions.defaultBuilt
            } else {
                DetailNavOptions.default
                    .setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_left)
                    .setPopExitAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_out_right)
                    .build()
            }
        )
    }
}
