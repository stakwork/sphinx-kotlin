package chat.sphinx.example.delete_media_detail.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class DeleteAllNotificationViewStateContainer : ViewState<DeleteAllNotificationViewStateContainer>() {

    object Closed: DeleteAllNotificationViewStateContainer()
    object Open: DeleteAllNotificationViewStateContainer()
    object Deleting: DeleteAllNotificationViewStateContainer()
    object Deleted: DeleteAllNotificationViewStateContainer()

}
