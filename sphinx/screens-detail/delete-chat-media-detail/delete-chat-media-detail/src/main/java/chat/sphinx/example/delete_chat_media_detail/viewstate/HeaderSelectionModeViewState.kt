package chat.sphinx.example.delete_chat_media_detail.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class HeaderSelectionModeViewState: ViewState<HeaderSelectionModeViewState>() {

    object Off : HeaderSelectionModeViewState()

    data class On (
        val itemsNumber: String,
        val sizeToDelete: String,
    ) : HeaderSelectionModeViewState()
}
