package chat.sphinx.example.delete_media_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.delete.media.detail.R
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.example.delete_media_detail.ui.DeletePodcastDetailFragmentArgs
import chat.sphinx.wrapper_common.feed.FeedId
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToDeleteMediaDetail(
    private val feedId: FeedId,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.delete_media_detail_nav_graph,
            DeletePodcastDetailFragmentArgs.Builder(feedId.value).build().toBundle(),
            DetailNavOptions.default
                .setEnterAnim(nav_R.anim.slide_in_left)
                .setPopExitAnim(nav_R.anim.slide_out_right)
                .build()
        )
    }

}
