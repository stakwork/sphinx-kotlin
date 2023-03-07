package chat.sphinx.episode_detail.ui

import io.matthewnelson.concept_views.viewstate.ViewState


internal sealed class EpisodeDetailViewState: ViewState<EpisodeDetailViewState>() {

    object Idle: EpisodeDetailViewState()
}
