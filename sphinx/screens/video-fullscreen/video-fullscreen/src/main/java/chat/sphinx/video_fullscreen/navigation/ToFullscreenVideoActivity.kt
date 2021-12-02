package chat.sphinx.video_fullscreen.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.video_fullscreen.R
import chat.sphinx.video_fullscreen.ui.activity.FullscreenVideoActivityArgs
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.message.MessageId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToFullscreenVideoActivity(
    private val messageId: MessageId,
    private val videoFilepath: String?,
    private val feedItemId: FeedId? = null,
    private val currentTime: Long = 0L
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
                .setArgFeedItemId(feedItemId?.value)
                .setArgVideoFilepath(videoFilepath)
                .build().toBundle(),
            options
        )
    }
}
