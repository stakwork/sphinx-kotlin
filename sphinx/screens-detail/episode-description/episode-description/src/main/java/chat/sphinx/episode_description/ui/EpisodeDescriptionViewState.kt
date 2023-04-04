package chat.sphinx.episode_description.ui

import io.matthewnelson.concept_views.viewstate.ViewState


internal sealed class EpisodeDescriptionViewState: ViewState<EpisodeDescriptionViewState>() {

    object Idle: EpisodeDescriptionViewState()

}
