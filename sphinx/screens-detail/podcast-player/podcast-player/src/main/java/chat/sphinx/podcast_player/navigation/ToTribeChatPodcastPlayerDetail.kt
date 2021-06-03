package chat.sphinx.podcast_player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.podcast_player.R
import chat.sphinx.detail_resources.DetailNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest


class ToTribeChatPodcastPlayerDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
) : NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.tribe_chat_podcast_player_nav_graph,
            null,
            options
        )
    }
}
