package chat.sphinx.delete_chat_media.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteChatMediaViewState: ViewState<DeleteChatMediaViewState>() {
    object Loading : DeleteChatMediaViewState()

    class ChatList(
        val section: List<String>,
        val totalSizeAllSections: String?
    ) : DeleteChatMediaViewState()



}
