package chat.sphinx.delete_chat_media.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteChatNotificationViewState: ViewState<DeleteChatNotificationViewState>() {

    object Closed : DeleteChatNotificationViewState()
    object Open : DeleteChatNotificationViewState()
    object Deleting : DeleteChatNotificationViewState()
    object SuccessfullyDeleted : DeleteChatNotificationViewState()
}
