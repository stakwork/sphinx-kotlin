package chat.sphinx.concept_repository_message

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_message.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun getMessageById(messageId: MessageId): Flow<Message?>

    suspend fun getMessagesForChat(chatId: ChatId): Flow<List<Message>>

    fun networkRefreshMessages(): Flow<LoadResponse<Boolean, ResponseError>>

    suspend fun readMessages(chatId: ChatId)
}
