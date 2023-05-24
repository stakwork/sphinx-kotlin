package chat.sphinx.example.delete_media.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteMediaViewState: ViewState<DeleteMediaViewState>() {

    object Idle : DeleteMediaViewState()


}
