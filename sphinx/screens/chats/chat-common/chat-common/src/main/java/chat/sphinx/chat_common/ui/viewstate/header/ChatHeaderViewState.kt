package chat.sphinx.chat_common.ui.viewstate.header

import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ChatHeaderViewState: ViewState<ChatHeaderViewState>() {

    object Idle: ChatHeaderViewState()

    data class Initialized(
        val chatHeaderName: String,
        val showLock: Boolean,

        // For Tribes only. If `null`, will set view to GONE
        val contributions: Sat?,

        // Will be null for contacts w/o a chat
        val isMuted: ChatMuted?,
    ): ChatHeaderViewState()
}
