package chat.sphinx.video_fullscreen.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.video_fullscreen.R
import chat.sphinx.video_fullscreen.ui.activity.FullscreenVideoActivityArgs
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.message.MessageId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToFullScreenVideoActivity(
    private val messageId: MessageId,
    private val videoFilepath: String?,
    private val feedId: FeedId? = null,
    private val currentTime: Int = 0
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val options = DetailNavOptions.default
            .setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_bottom)
            .build()

        controller.navigate(
            R.id.fullscreen_video_nav_graph,
            FullscreenVideoActivityArgs.Builder(
                messageId.value,
                currentTime
            )
                .setArgFeedId(feedId?.value)
                .setArgVideoFilepath(videoFilepath)
                .build().toBundle(),
            options
        )
    }
}
