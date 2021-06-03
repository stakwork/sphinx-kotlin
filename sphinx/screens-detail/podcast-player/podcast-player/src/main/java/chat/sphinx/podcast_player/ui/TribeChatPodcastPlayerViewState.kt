package chat.sphinx.podcast_player.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TribeChatPodcastPlayerViewState: ViewState<TribeChatPodcastPlayerViewState>() {
    object Idle: TribeChatPodcastPlayerViewState()
}
