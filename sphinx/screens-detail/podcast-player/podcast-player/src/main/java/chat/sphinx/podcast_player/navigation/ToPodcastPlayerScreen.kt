package chat.sphinx.podcast_player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.podcast_player.R
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.podcast_player.ui.PodcastPlayerFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.NavigationRequest

class ToPodcastPlayerScreen(
    private val chatId: ChatId,
    private val feedId: FeedId,
    private val feedUrl: FeedUrl,
    private val fromFeed: Boolean,
    private val fromDownloaded: Boolean,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
) : NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.podcast_player_nav_graph,
            PodcastPlayerFragmentArgs.Builder(
                chatId.value,
                feedId.value,
                feedUrl.value,
                fromFeed,
                fromDownloaded
            ).build().toBundle(),
            options
        )
    }
}