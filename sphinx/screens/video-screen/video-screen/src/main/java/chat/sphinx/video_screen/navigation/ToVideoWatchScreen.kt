package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.ui.watch.VideoFeedWatchScreenFragmentArgs
import chat.sphinx.wrapper_common.feed.FeedId
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToVideoWatchScreen(
    val feedItemId: FeedId
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val builder = VideoFeedWatchScreenFragmentArgs.Builder(
            feedItemId.value
        )

        controller.navigate(
            R.id.video_watch_nav_graph,
            builder.build().toBundle(),
            DefaultNavOptions.defaultAnimsBuilt
        )
    }
}