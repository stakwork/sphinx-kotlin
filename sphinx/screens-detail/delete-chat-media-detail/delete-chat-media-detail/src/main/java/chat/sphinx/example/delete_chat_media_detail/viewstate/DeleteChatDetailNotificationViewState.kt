package chat.sphinx.example.delete_chat_media_detail.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteChatDetailNotificationViewState: ViewState<DeleteChatDetailNotificationViewState>() {

    object Closed : DeleteChatDetailNotificationViewState()
    object Open : DeleteChatDetailNotificationViewState()
    object Deleting : DeleteChatDetailNotificationViewState()
    object SuccessfullyDeleted : DeleteChatDetailNotificationViewState()
}