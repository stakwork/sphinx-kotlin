package chat.sphinx.concept_repository_media

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import kotlinx.coroutines.flow.Flow

interface RepositoryMedia {
    fun getChatById(chatId: ChatId): Flow<Chat?>
}
