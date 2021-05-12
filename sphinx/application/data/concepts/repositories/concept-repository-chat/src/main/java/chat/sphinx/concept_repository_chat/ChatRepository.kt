package chat.sphinx.concept_repository_chat

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.chat.ChatId
import kotlinx.coroutines.flow.Flow

/**
 * All [Chat]s are cached to the DB such that a network refresh will update
 * them, and thus proc any [Flow] being collected
 * */
interface ChatRepository {
    suspend fun getUnseenMessagesByChatId(chat: Chat): Flow<Long?>
    suspend fun getChats(): Flow<List<Chat>>
    suspend fun getChatById(chatId: ChatId): Flow<Chat?>
    suspend fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?>
    fun networkRefreshChats(): Flow<LoadResponse<Boolean, ResponseError>>
}
