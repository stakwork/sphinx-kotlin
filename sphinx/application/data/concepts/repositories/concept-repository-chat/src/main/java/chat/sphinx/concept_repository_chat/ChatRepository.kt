package chat.sphinx.concept_repository_chat

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatUUID
import chat.sphinx.wrapper_common.chat.ChatId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * All [Chat]s are cached to the DB such that a network refresh will update
 * them, and thus proc any [Flow] being collected
 * */
interface ChatRepository {

    @Throws(CancellationException::class)
    suspend fun getChats(): Flow<List<Chat>>

    @Throws(CancellationException::class)
    suspend fun getChatById(chatId: ChatId): Flow<Chat?>

    @Throws(CancellationException::class)
    suspend fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?>

    @Throws(CancellationException::class)
    fun networkRefreshChats(): Flow<LoadResponse<Boolean, ResponseError>>
}
