package chat.sphinx.notification_level.ui

import chat.sphinx.wrapper_chat.NotificationLevel
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class NotificationLevelViewState: ViewState<NotificationLevelViewState>() {
    class ChatNotificationLevel(
        val level: NotificationLevel?,
    ): NotificationLevelViewState()
}
