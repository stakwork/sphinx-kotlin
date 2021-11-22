package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.ui.VideoScreenFragmentArgs
import chat.sphinx.wrapper_common.feed.FeedId
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToVideoScreen(
    val feedId: FeedId,
    val feedItemId: FeedId? = null
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val builder = VideoScreenFragmentArgs.Builder(
            feedId.value
        )
        builder.argFeedItemId = feedItemId?.value

        controller.navigate(
            R.id.video_screen_nav_graph,
            builder.build().toBundle(),
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}