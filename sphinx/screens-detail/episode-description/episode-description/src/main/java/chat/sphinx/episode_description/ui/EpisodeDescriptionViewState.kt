package chat.sphinx.episode_description.ui

import chat.sphinx.episode_description.model.EpisodeDescription
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState


internal sealed class EpisodeDescriptionViewState: ViewState<EpisodeDescriptionViewState>() {

    object Idle: EpisodeDescriptionViewState()

    data class FeedItemDescription(
        val feedItemDescription: EpisodeDescription
    ) : EpisodeDescriptionViewState()

    data class FeedItemDetails(
        val feedItemDescription: FeedItemDescriptionDetailsViewState
    ) : EpisodeDescriptionViewState()


}
