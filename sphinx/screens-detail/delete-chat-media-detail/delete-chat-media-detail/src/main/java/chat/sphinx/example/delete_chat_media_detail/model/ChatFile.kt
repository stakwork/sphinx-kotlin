package chat.sphinx.example.delete_chat_media_detail.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.MessageId

data class ChatFile(
    val fileName: String?,
    val mediaType: String,
    val messageId: MessageId,
    val chatId: ChatId,
    val size: String,
    val isSelected: Boolean = false
    )
