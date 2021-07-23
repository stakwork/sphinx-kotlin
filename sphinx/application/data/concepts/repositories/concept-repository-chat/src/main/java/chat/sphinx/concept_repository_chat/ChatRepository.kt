package chat.sphinx.concept_repository_chat

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatAlias
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_message_media.MediaType
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * All [Chat]s are cached to the DB such that a network refresh will update
 * them, and thus proc any [Flow] being collected
 * */
interface ChatRepository {
    val getAllChats: Flow<List<Chat>>
    fun getChatById(chatId: ChatId): Flow<Chat?>
    fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?>

    /**
     * Returns a [chat.sphinx.wrapper_chat.ChatType.Conversation] or `null`
     * for the provided [contactId]
     * */
    fun getConversationByContactId(contactId: ContactId): Flow<Chat?>

    /**
     * Throws [NoSuchElementException] on collection if [Chat.contactIds]
     * is empty.
     * */
    fun getUnseenMessagesByChatId(chatId: ChatId): Flow<Long?>
    
    val networkRefreshChats: Flow<LoadResponse<Boolean, ResponseError>>

    suspend fun getAllChatsByIds(chatIds: List<ChatId>): List<Chat>
    /**
     * Returns `true` if the user has muted the chat and there is a need
     * to notify them that they won't receive messages anymore.
     *
     * Returns `false` if the user has _un_ muted the chat and there is no
     * need to notify.
     *
     * Returns error if something went wrong (networking)
     * */
    suspend fun toggleChatMuted(chat: Chat): Response<Boolean, ResponseError>

    fun joinTribe(
        tribeDto: TribeDto,
    ): Flow<LoadResponse<Any, ResponseError>>

    suspend fun updateTribeInfo(chat: Chat): PodcastDto?

    suspend fun exitAndDeleteTribe(chat: Chat): Response<Boolean, ResponseError>

    suspend fun updateChatProfilePic(
        chat: Chat,
        stream: InputStreamProvider,
        mediaType: MediaType,
        fileName: String,
        contentLength: Long?
    ): Response<ChatDto, ResponseError>

    suspend fun updateChatProfileAlias(
        chatId: ChatId,
        alias: ChatAlias?
    ): Response<ChatDto, ResponseError>
}
