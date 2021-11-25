package chat.sphinx.newsletter_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.newsletter_detail.R
import chat.sphinx.newsletter_detail.ui.NewsletterDetailFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.NavigationRequest

class ToNewsletterDetailScreen(
    private val chatId: ChatId,
    private val feedUrl: FeedUrl,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.newsletter_detail_nav_graph,
            NewsletterDetailFragmentArgs.Builder(chatId.value, feedUrl.value)
                .build()
                .toBundle(),
            options
        )
    }
}
