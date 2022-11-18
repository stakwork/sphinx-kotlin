package chat.sphinx.common_player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.common_player.R
import chat.sphinx.common_player.ui.CommonPlayerScreenFragmentArgs
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.NavigationRequest

class ToCommonPlayerScreen(
    private val chatId: ChatId,
    private val feedId: FeedId,
    private val feedUrl: FeedUrl,
    private val fromFeed: Boolean,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
) : NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.common_player_nav_graph,
            CommonPlayerScreenFragmentArgs.Builder(
                chatId.value,
                feedId.value,
                feedUrl.value,
                fromFeed
            ).build().toBundle(),
            options
        )
    }
}