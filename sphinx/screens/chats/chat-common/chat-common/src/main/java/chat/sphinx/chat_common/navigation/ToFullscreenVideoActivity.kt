package chat.sphinx.chat_common.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.ui.activity.FullscreenVideoActivityArgs
import chat.sphinx.wrapper_common.message.MessageId
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToFullscreenVideoActivity(
    private val messageId: MessageId,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.fullscreen_video_nav_graph,
            FullscreenVideoActivityArgs.Builder(messageId.value).build().toBundle(),
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}
