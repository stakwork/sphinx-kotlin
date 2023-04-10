package chat.sphinx.episode_description.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.create_description.R
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.episode_description.ui.EpisodeDescriptionFragmentArgs
import chat.sphinx.wrapper_common.feed.FeedId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToEpisodeDescription(
    private val feedItemId: FeedId,
    private val isRecommendation: Boolean
) : NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        val args = EpisodeDescriptionFragmentArgs.Builder(feedItemId.value, isRecommendation)

        controller.navigate(
            R.id.episode_description_nav_graph,
            args.build().toBundle(),
            DetailNavOptions.default.apply {
                setEnterAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_in_left)
                setPopExitAnim(io.matthewnelson.android_feature_navigation.R.anim.slide_out_right)
            }.build()
        )
    }
}
