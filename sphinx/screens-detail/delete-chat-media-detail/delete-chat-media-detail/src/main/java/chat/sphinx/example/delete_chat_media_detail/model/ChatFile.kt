package chat.sphinx.example.delete_chat_media_detail.model

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.MessageId
import java.io.File

data class ChatFile(
    val fileName: String?,
    val mediaType: String,
    val messageId: MessageId,
    val chatId: ChatId,
    val size: String,
    val isSelected: Boolean = false,
    val localFile: File?,
    val ext: String?
    )
