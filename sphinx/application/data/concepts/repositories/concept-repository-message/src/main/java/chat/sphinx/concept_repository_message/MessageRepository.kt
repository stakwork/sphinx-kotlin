package chat.sphinx.concept_repository_message

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_message.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun getLatestMessageForChatById(chatId: ChatId): Flow<Message?>
    suspend fun getLatestMessageForChatByUUID(chatUUID: ChatUUID): Flow<Message?>

    suspend fun getNumberUnseenMessagesForChatById(chatId: ChatId): Flow<Int>
    suspend fun getNumberUnseenMessagesForChatByUUID(chatUUID: ChatUUID): Flow<Int>

    suspend fun getMessagesForChatById(chatId: ChatId, limit: Int, offset: Int): Flow<List<Message>>
    suspend fun getMessagesForChatByUUID(chatUUID: ChatUUID, limit: Int, offset: Int): Flow<List<Message>>
    fun networkRefreshMessages(): Flow<LoadResponse<Boolean, ResponseError>>
}
