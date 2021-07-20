package chat.sphinx.tribe_detail.ui

import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.wrapper_chat.Chat
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TribeDetailViewState: ViewState<TribeDetailViewState>() {
    object Idle: TribeDetailViewState()

    class Tribe(
        val chat: Chat,
        val podcast: Podcast?
    ): TribeDetailViewState()
}
