package chat.sphinx.example.delete_media_detail.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteMediaDetailViewState : ViewState<DeleteMediaDetailViewState>() {
    object Idle : DeleteMediaDetailViewState()
}
