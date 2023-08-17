package chat.sphinx.episode_detail.ui

import chat.sphinx.episode_detail.model.EpisodeDetail
import io.matthewnelson.concept_views.viewstate.ViewState


internal sealed class EpisodeDetailViewState: ViewState<EpisodeDetailViewState>() {

    object Idle: EpisodeDetailViewState()

    data class ShowEpisode(
        val episodeDetail: EpisodeDetail
    ): EpisodeDetailViewState()

}
