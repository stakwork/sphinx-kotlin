package chat.sphinx.delete_chat_media.viewstate

import chat.sphinx.delete_chat_media.model.ChatToDelete
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteChatMediaViewState: ViewState<DeleteChatMediaViewState>() {
    object Loading : DeleteChatMediaViewState()

    class ChatList(
        val chats: List<ChatToDelete>,
        val totalSizeChats: String?
    ) : DeleteChatMediaViewState()

}
