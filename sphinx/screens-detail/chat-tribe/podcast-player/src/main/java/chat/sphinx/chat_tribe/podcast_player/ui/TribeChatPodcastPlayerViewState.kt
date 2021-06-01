package chat.sphinx.chat_tribe.podcast_player.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TribeChatPodcastPlayerViewState: ViewState<TribeChatPodcastPlayerViewState>() {
    object Idle: TribeChatPodcastPlayerViewState()
}
