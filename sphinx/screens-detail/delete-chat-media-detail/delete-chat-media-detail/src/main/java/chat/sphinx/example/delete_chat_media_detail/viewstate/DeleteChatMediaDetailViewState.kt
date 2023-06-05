package chat.sphinx.example.delete_chat_media_detail.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteChatMediaDetailViewState: ViewState<DeleteChatMediaDetailViewState>() {
    object Loading : DeleteChatMediaDetailViewState()

    class FileList(
        val files: List<String>,
        val totalSizeFiles: String?
    ) : DeleteChatMediaDetailViewState()

}
