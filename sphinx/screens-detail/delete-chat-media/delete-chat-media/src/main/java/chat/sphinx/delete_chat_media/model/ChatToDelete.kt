package chat.sphinx.delete_chat_media.model

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_contact.ContactAlias

data class ChatToDelete(
    val contactAlias: String,
    val photoUrl: PhotoUrl?,
    val size: String,
    val chatId: ChatId,
    val initials: Initials
)

data class Initials(
    val initials: String?,
    val colorKey: String
)