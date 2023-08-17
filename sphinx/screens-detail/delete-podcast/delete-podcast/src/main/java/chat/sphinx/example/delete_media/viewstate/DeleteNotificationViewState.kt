package chat.sphinx.example.delete_media.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteNotificationViewState: ViewState<DeleteNotificationViewState>() {

    object Closed : DeleteNotificationViewState()
    object Open : DeleteNotificationViewState()
    object Deleting : DeleteNotificationViewState()
    object SuccessfullyDeleted : DeleteNotificationViewState()
}
