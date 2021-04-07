package chat.sphinx.dashboard.ui.adapter

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_message.Message

data class DashboardChat(
    val chat: Chat,
    val message: Message?,
)
