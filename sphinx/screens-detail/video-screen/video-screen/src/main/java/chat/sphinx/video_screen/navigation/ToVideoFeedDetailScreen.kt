package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.video_screen.R
import chat.sphinx.video_screen.ui.detail.VideoFeedDetailScreenFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.NavigationRequest

class ToVideoFeedDetailScreen(
    private val chatId: ChatId,
    private val feedId: FeedId,
    private val feedUrl: FeedUrl,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
) : NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.video_feed_details_nav_graph,
            VideoFeedDetailScreenFragmentArgs.Builder(
                chatId.value,
                feedId.value,
                feedUrl.value,
            ).build().toBundle(),
            options
        )
    }
}