package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.ui.detail.VideoFeedDetailScreenFragmentArgs
import chat.sphinx.wrapper_common.feed.FeedId
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToVideoFeedDetailScreen(
    val feedId: FeedId
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val builder = VideoFeedDetailScreenFragmentArgs.Builder(
            feedId.value
        )

        controller.navigate(
            R.id.video_feed_details_nav_graph,
            builder.build().toBundle(),
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}