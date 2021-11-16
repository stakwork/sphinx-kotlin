package chat.sphinx.web_view.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.web_view.R
import chat.sphinx.web_view.ui.WebViewFragmentArgs
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.NavigationRequest

class ToWebViewDetail(
    private val title: String,
    private val url: FeedUrl,
    private val fromList: Boolean,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.web_view_nav_graph,
            WebViewFragmentArgs.Builder(title, url.value, fromList)
                .build()
                .toBundle(),
            options
        )
    }
}
