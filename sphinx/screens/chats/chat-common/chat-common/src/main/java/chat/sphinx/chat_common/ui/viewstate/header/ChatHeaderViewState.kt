package chat.sphinx.chat_common.ui.viewstate.header

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_common.lightning.Sat
import io.matthewnelson.concept_views.viewstate.ViewState

@Suppress("NOTHING_TO_INLINE")
inline fun ChatHeaderViewState.Initialized.cloneWithNewParams(
    initialHolderViewState: InitialHolderViewState? = null,
    chatHeaderName: String? = null,
    showLock: Boolean? = null,
    connectivity: LoadResponse<Boolean, ResponseError>? = null,
    contributions: Sat? = null,
    isMuted: ChatMuted? = null
): ChatHeaderViewState =
    ChatHeaderViewState.Initialized(
        initialHolderViewState ?: this.initialHolderViewState,
        chatHeaderName ?: this.chatHeaderName,
        showLock ?: this.showLock,
        connectivity ?: this.connectivity,
        contributions ?: this.contributions,
        isMuted ?: this.isMuted,
    )

sealed class ChatHeaderViewState: ViewState<ChatHeaderViewState>() {
    object Idle: ChatHeaderViewState()
    data class Initialized(
        val initialHolderViewState: InitialHolderViewState,
        val chatHeaderName: String,
        val showLock: Boolean,
        val connectivity: LoadResponse<Boolean, ResponseError>,

        // For Tribes only. If `null`, will set view to GONE
        val contributions: Sat?,

        // Will be null for contacts w/o a chat
        val isMuted: ChatMuted?,
    ): ChatHeaderViewState()
}
