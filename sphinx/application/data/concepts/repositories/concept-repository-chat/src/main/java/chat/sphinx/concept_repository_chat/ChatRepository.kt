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
    val getAllChats: Flow<List<Chat>>
    fun getChatById(chatId: ChatId): Flow<Chat?>
    fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?>

    /**
     * Throws [NoSuchElementException] on collection if [Chat.contactIds]
     * is empty.
     * */
    fun getUnseenMessagesByChatId(chat: Chat): Flow<Long?>
    val networkRefreshChats: Flow<LoadResponse<Boolean, ResponseError>>

    suspend fun toggleChatMuted(chat: Chat): Flow<Chat?>
}
