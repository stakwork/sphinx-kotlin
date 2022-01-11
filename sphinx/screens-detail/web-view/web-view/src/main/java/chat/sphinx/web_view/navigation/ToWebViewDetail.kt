package chat.sphinx.web_view.navigation

import androidx.navigation.NavController
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.web_view.R
import chat.sphinx.web_view.ui.WebViewFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToWebViewDetail(
    private val chatId: ChatId?,
    private val title: String,
    private val url: FeedUrl,
    private val feedId: FeedId?,
    private val feedItemId: FeedId?,
    private val fromList: Boolean,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        
        val navOptions = if (fromList) {
            DetailNavOptions.default.apply {
                setEnterAnim(nav_R.anim.slide_in_left)
                setPopExitAnim(nav_R.anim.slide_out_right)
            }.build()
        } else {
            DetailNavOptions.defaultBuilt
        }

        controller.navigate(
            R.id.web_view_nav_graph,
            WebViewFragmentArgs.Builder(
                chatId?.value ?: ChatId.NULL_CHAT_ID.toLong(),
                title,
                url.value,
                feedId?.value,
                feedItemId?.value,
                fromList
            ).build().toBundle(),
            navOptions
        )
    }
}
