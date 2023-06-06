package chat.sphinx.example.delete_chat_media_detail.viewstate

import chat.sphinx.example.delete_chat_media_detail.model.ChatFile
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteChatMediaDetailViewState: ViewState<DeleteChatMediaDetailViewState>() {
    object Loading : DeleteChatMediaDetailViewState()

    class FileList(
        val files: List<ChatFile>,
        val totalSizeFiles: String?
    ) : DeleteChatMediaDetailViewState()

}
