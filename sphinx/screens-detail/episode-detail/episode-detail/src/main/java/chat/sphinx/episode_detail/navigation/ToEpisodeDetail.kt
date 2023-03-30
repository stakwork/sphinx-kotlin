package chat.sphinx.episode_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.episode_detail.R
import chat.sphinx.episode_detail.ui.EpisodeDetailFragment
import chat.sphinx.episode_detail.ui.EpisodeDetailFragmentArgs
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import io.matthewnelson.concept_navigation.NavigationRequest

class ToEpisodeDetail(
    private val feedItemId: FeedId?,
    private val header: String,
    private val image: String,
    private val episodeTypeImage: Int,
    private val episodeTypeText: String,
    private val episodeDate: String,
    private val episodeDuration: String,
    private val downloaded: Boolean?,
    private val link: FeedUrl?,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
) : NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {

        val args = EpisodeDetailFragmentArgs.Builder(
            header,
            image,
            episodeTypeImage,
            episodeTypeText,
            episodeDate,
            episodeDuration,
            downloaded ?: false
        )
        args.argFeedId = feedItemId?.value
        args.argLink = link?.value

        controller.navigate(
            R.id.episode_detail_nav_graph,
            args.build().toBundle(),
            options
        )
    }
}
