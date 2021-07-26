package chat.sphinx.tribe_detail.ui

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_podcast.Podcast
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class TribeDetailViewState: ViewState<TribeDetailViewState>() {
    object Idle: TribeDetailViewState()

    class TribeProfile(
        val chat: Chat,
        val accountOwner: Contact,
        val podcast: Podcast?
    ): TribeDetailViewState()
}
