package chat.sphinx.concept_repository_message

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_message.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun getLatestMessageForChat(chatId: ChatId): Flow<Message?>
    suspend fun getLatestMessageForChat(chatUUID: ChatUUID): Flow<Message?>

    suspend fun getNumberUnseenMessagesForChat(chatId: ChatId): Flow<Int>
    suspend fun getNumberUnseenMessagesForChat(chatUUID: ChatUUID): Flow<Int>

    suspend fun getMessagesForChat(chatId: ChatId, limit: Int, offset: Int): Flow<List<Message>>
    suspend fun getMessagesForChat(chatUUID: ChatUUID, limit: Int, offset: Int): Flow<List<Message>>
    fun networkRefreshMessages(): Flow<LoadResponse<Boolean, ResponseError>>
}
