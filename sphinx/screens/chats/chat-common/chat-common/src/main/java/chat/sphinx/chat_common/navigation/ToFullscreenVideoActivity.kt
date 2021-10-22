package chat.sphinx.chat_common.navigation

import androidx.navigation.NavController
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.ui.activity.FullscreenVideoActivityArgs
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.wrapper_common.message.MessageId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToFullscreenVideoActivity(
    private val messageId: MessageId,
    private val videoFilepath: String?
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val options = DetailNavOptions.default
            .setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_bottom)
            .build()

        controller.navigate(
            R.id.fullscreen_video_nav_graph,
            FullscreenVideoActivityArgs.Builder(messageId.value)
                .setArgVideoFilepath(videoFilepath)
                .build().toBundle(),
            options
        )
    }
}
