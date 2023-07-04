package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.ui.watch.VideoFeedWatchScreenFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.concept_navigation.NavigationRequest

class ToVideoWatchScreen(
    private val chatId: ChatId,
    private val feedId: FeedId,
    private val feedUrl: FeedUrl,
    private val feedItem: FeedId? = null,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
) : NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        val args = VideoFeedWatchScreenFragmentArgs.Builder(
            chatId.value,
            feedId.value,
            feedUrl.value,
        )
        args.argFeedItemId = feedItem?.value

        controller.navigate(
            R.id.video_watch_nav_graph,
            args.build().toBundle(),
            options
        )
    }
}