package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_coredb.util.upsertChat
import chat.sphinx.concept_coredb.util.upsertMessage
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_crypto_rsa.RsaPrivateKey
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
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageContent
import chat.sphinx.wrapper_message.MessageType
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.k_openssl_common.annotations.RawPasswordAccess
import io.matthewnelson.k_openssl_common.annotations.UnencryptedDataAccess
import io.matthewnelson.k_openssl_common.clazzes.EncryptedString
import io.matthewnelson.k_openssl_common.clazzes.UnencryptedByteArray
import io.matthewnelson.k_openssl_common.clazzes.toUnencryptedString
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
            .mapToList(dispatchers.io)
            .map { chatDboPresenterMapper.mapListFrom(it) }
    }

    override suspend fun getChatById(chatId: ChatId): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().getChatById(chatId)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().getChatByUUID(chatUUID)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
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

    @OptIn(RawPasswordAccess::class)
    private suspend fun decryptMessageContent(
        messageContent: MessageContent
    ): Response<UnencryptedByteArray, ResponseError> =
        authenticationCoreManager.getEncryptionKey()?.let { keys ->
            rsa.decrypt(
                rsaPrivateKey = RsaPrivateKey(keys.privateKey.value),
                text = EncryptedString(messageContent.value),
                dispatcher = dispatchers.default
            )
        } ?: Response.Error(ResponseError("EncryptionKey retrieval failed"))

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

    @OptIn(UnencryptedDataAccess::class)
    override fun networkRefreshMessages(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        emit(LoadResponse.Loading)
        val queries = coreDB.getSphinxDatabaseQueries()
        var lastMessageId: Long = withContext(dispatchers.io) {
            queries.getLatestMessageId().executeAsOneOrNull()?.value ?: 0
        }

        val supervisor = SupervisorJob(currentCoroutineContext().job)
        val scope = CoroutineScope(supervisor)

        var error: Response.Error<ResponseError>? = null

        while (currentCoroutineContext().isActive && lastMessageId >= 0) {

            networkQueryMessage.getMessages(
                MessagePagination.instantiate(200, lastMessageId.toInt(), null)
            ).collect { response ->
                @Exhaustive
                when (response) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        lastMessageId = -1
                        error = response
                    }
                    is Response.Success -> {
                        val newMessages = response.value.new_messages

                        if (newMessages.isNotEmpty()) {
                            val decryptMap: MutableMap<MessageId, String> = mutableMapOf()

                            for (message in newMessages) {
                                message.message_content?.let { content ->
                                    if (content.isNotEmpty() && message.type != MessageType.KEY_SEND) {
                                        decryptMap[MessageId(message.id)] = content
                                    }
                                }
                            }

                            // TODO: Move out to separate class to encapsulate
                            val decryptMapCounterLock = Mutex()
                            var counter = decryptMap.size

                            for (key in decryptMap.keys) {
                                scope.launch decrypt@ {
                                    val content: String = decryptMapCounterLock.withLock {
                                        decryptMap[key] ?: let {
                                            counter--
                                            return@decrypt
                                        }
                                    }

                                    val decrypted = decryptMessageContent(MessageContent(content))

                                    decryptMapCounterLock.withLock {
                                        @Exhaustive
                                        when (decrypted) {
                                            is Response.Error -> {
                                                decryptMap.remove(key)
                                                counter--
                                            }
                                            is Response.Success -> {
                                                decryptMap[key] =
                                                    decrypted.value.toUnencryptedString().value
                                                counter--
                                            }
                                        }
                                    }
                                }
                            }

                            while (currentCoroutineContext().isActive) {
                                delay(100L)
                                if (decryptMapCounterLock.withLock { counter } <= 1) {
                                    break
                                }
                            }

                            for (message in newMessages) {
                                decryptMap[MessageId(message.id)]?.let { decrypted ->
                                    message.setMessageContentDecrypted(decrypted)
                                }
                            }

                            val dbos = messageDtoDboMapper.mapListFrom(newMessages)

                            chatLock.withLock {
                                messageLock.withLock {
                                    withContext(dispatchers.io) {

                                        queries.transaction {
                                            val chatIds = queries.getAllChatIds().executeAsList()
                                            for (dbo in dbos) {
                                                if (dbo.chat_id.value == MessageDtoDboMapper.NULL_CHAT_ID.toLong()) {
                                                    queries.upsertMessage(dbo)
                                                } else if (chatIds.contains(dbo.chat_id)) {
                                                    queries.upsertMessage(dbo)
                                                }
                                            }
                                        }

                                    }
                                }
                            }

                        }

                        when {
                            lastMessageId == -1 -> {}
                            newMessages.size >= 199 -> {
                                lastMessageId += 200
                            }
                            else -> {
                                lastMessageId = -1
                            }
                        }
                    }
                }
            }
        }

        supervisor.cancelAndJoin()

        error?.let { responseError ->
            emit(responseError)
        } ?: emit(Response.Success(true))
    }
}
