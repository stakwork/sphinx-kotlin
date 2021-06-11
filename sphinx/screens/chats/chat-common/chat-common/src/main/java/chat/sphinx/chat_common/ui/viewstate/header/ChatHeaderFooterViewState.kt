package chat.sphinx.chat_common.ui.viewstate.header

import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ChatHeaderFooterViewState: ViewState<ChatHeaderFooterViewState>() {

    object Idle: ChatHeaderFooterViewState()

    data class Initialized(
        val chatHeaderName: String,
        val showLock: Boolean,
        val contributions: Sat?,
        val isMuted: ChatMuted?,
    ): ChatHeaderFooterViewState()

    data class PodcastUpdate(
        val podcast: Podcast,
    ): ChatHeaderFooterViewState()
}
