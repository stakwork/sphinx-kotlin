package chat.sphinx.tribe_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.tribe_detail.R
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToTribeDetailScreen(
    chatId: ChatId,
    podcast: Podcast?,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.tribe_detail_nav_graph,
            null,
            options
        )
    }
}
