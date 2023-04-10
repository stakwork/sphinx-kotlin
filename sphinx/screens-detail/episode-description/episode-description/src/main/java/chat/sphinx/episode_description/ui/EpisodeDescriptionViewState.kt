package chat.sphinx.episode_description.ui

import chat.sphinx.episode_description.model.FeedItemDescription
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class EpisodeDescriptionViewState: ViewState<EpisodeDescriptionViewState>() {

    object Idle: EpisodeDescriptionViewState()

    data class ItemDescription(
        val feedItemDescription: FeedItemDescription
    ) : EpisodeDescriptionViewState()
}
