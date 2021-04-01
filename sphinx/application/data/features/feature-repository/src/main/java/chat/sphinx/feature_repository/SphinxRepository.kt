package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_coredb.util.upsertChat
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.feature_repository.mappers.chat.ChatDboPresenterMapper
import chat.sphinx.feature_repository.mappers.chat.ChatDtoDboMapper
import chat.sphinx.feature_repository.mappers.mapListFrom
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.feature_repository.mappers.message.MessageDboPresenterMapper
import chat.sphinx.feature_repository.mappers.message.MessageDtoDboMapper
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_message.Message
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.ParseException

class SphinxRepository(
    private val authenticationCoreManager: AuthenticationCoreManager,
    private val coreDB: CoreDB,
    private val dispatchers: CoroutineDispatchers,
    private val networkQueryChat: NetworkQueryChat,
    private val networkQueryMessage: NetworkQueryMessage,
    private val rsa: RSA,
): ChatRepository, MessageRepository {

    /////////////
    /// Chats ///
    /////////////
    private val chatLock = Mutex()
    private val chatDtoDboMapper: ChatDtoDboMapper by lazy {
        ChatDtoDboMapper(dispatchers)
    }
    private val chatDboPresenterMapper: ChatDboPresenterMapper by lazy {
        ChatDboPresenterMapper(dispatchers)
    }

    override suspend fun getChats(): Flow<List<Chat>> {
        return coreDB.getSphinxDatabaseQueries().getAllChats()
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToList(dispatchers.default)
            .map { chatDboPresenterMapper.mapListFrom(it) }
            .flowOn(dispatchers.default)
    }

    override suspend fun getChatById(chatId: ChatId): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().getChatById(chatId)
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToOneOrNull(dispatchers.default)
            .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().getChatByUUID(chatUUID)
            .asFlow()
            .flowOn(dispatchers.io)
            .mapToOneOrNull(dispatchers.default)
            .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override fun networkRefreshChats(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        networkQueryChat.getChats().collect { loadResponse ->

            @Exhaustive
            when (loadResponse) {
                is Response.Error -> {
                    emit(loadResponse)
                }
                is Response.Success -> {

                    try {

                        chatLock.withLock {

                            val dbos = chatDtoDboMapper.mapListFrom(loadResponse.value)

                            val queries = coreDB.getSphinxDatabaseQueries()

                            withContext(dispatchers.io) {

                                val chatIdsToRemove = queries.getAllChatIds()
                                    .executeAsList()
                                    .toMutableSet()

                                messageLock.withLock {

                                    queries.transaction {
                                        dbos.forEach { dbo ->
                                            queries.upsertChat(dbo)

                                            chatIdsToRemove.remove(dbo.id)
                                        }

                                        // remove remaining chat's from DB
                                        chatIdsToRemove.forEach { chatId ->
                                            queries.deleteChatById(chatId)
                                            queries.deleteMessagesByChatId(chatId)
                                        }

                                    }

                                }

                            }

                        }

                        emit(Response.Success(true))

                    } catch (e: IllegalArgumentException) {
                        emit(
                            Response.Error(
                                ResponseError("Failed to convert Json from Relay", e)
                            )
                        )
                    } catch (e: ParseException) {
                        emit(
                            Response.Error(
                                ResponseError("Failed to convert date/time from SphinxRelay", e)
                            )
                        )
                    }
                }
                is LoadResponse.Loading -> {
                    emit(loadResponse)
                }
            }

        }
    }

    ////////////////
    /// Messages ///
    ////////////////
    private val messageLock = Mutex()
    private val messageDtoDboMapper: MessageDtoDboMapper by lazy {
        MessageDtoDboMapper(dispatchers)
    }
    private val messageDboPresenterMapper: MessageDboPresenterMapper by lazy {
        MessageDboPresenterMapper(dispatchers)
    }

    override suspend fun getLatestMessageForChat(chatId: ChatId): Flow<Message?> {
        TODO("Not yet implemented")
    }

    override suspend fun getLatestMessageForChat(chatUUID: ChatUUID): Flow<Message?> {
        TODO("Not yet implemented")
    }

    override suspend fun getNumberUnseenMessagesForChat(chatId: ChatId): Flow<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun getNumberUnseenMessagesForChat(chatUUID: ChatUUID): Flow<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun getMessagesForChat(
        chatId: ChatId,
        limit: Int,
        offset: Int
    ): Flow<List<Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun getMessagesForChat(
        chatUUID: ChatUUID,
        limit: Int,
        offset: Int
    ): Flow<List<Message>> {
        TODO("Not yet implemented")
    }

    override fun networkRefreshMessages(): Flow<LoadResponse<Boolean, ResponseError>> {
        TODO("Not yet implemented")
    }
}
