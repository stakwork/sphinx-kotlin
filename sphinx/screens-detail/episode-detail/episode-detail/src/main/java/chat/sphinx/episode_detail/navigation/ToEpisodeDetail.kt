package chat.sphinx.episode_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.episode_detail.R
import chat.sphinx.episode_detail.ui.EpisodeDetailFragment
import chat.sphinx.episode_detail.ui.EpisodeDetailFragmentArgs
import io.matthewnelson.concept_navigation.NavigationRequest

class ToEpisodeDetail(
    private val header: String,
    private val image: String,
    private val episodeTypeImage: Int,
    private val episodeTypeText: String,
    private val episodeDate: String,
    private val episodeDuration: String,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {

    private val args = EpisodeDetailFragmentArgs.Builder(
        header,
        image,
        episodeTypeImage,
        episodeTypeText,
        episodeDate,
        episodeDuration
    ).build().toBundle()

    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.episode_detail_nav_graph,
            args,
            options
        )
    }
}
