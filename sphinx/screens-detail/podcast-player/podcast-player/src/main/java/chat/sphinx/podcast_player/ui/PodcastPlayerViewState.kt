package chat.sphinx.podcast_player.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class PodcastPlayerViewState: ViewState<PodcastPlayerViewState>() {
    object Idle: PodcastPlayerViewState()
}
