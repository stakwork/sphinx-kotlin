package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_coredb.util.upsertChat
import chat.sphinx.concept_coredb.util.upsertContact
import chat.sphinx.concept_coredb.util.upsertMessage
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.wrapper_rsa.RsaPrivateKey
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.feature_repository.mappers.chat.ChatDboPresenterMapper
import chat.sphinx.feature_repository.mappers.chat.ChatDtoDboMapper
import chat.sphinx.feature_repository.mappers.contact.ContactDboPresenterMapper
import chat.sphinx.feature_repository.mappers.contact.ContactDtoDboMapper
import chat.sphinx.feature_repository.mappers.mapListFrom
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.feature_repository.mappers.message.MessageDboPresenterMapper
import chat.sphinx.feature_repository.mappers.message.MessageDtoDboMapper
import chat.sphinx.kotlin_response.exception
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.isTrue
import chat.sphinx.wrapper_contact.toContactFromGroup
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.MessageContent
import chat.sphinx.wrapper_message.MessageContentDecrypted
import chat.sphinx.wrapper_message.MessageType
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.annotations.UnencryptedDataAccess
import io.matthewnelson.crypto_common.clazzes.EncryptedString
import io.matthewnelson.crypto_common.clazzes.UnencryptedByteArray
import io.matthewnelson.crypto_common.clazzes.toUnencryptedString
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
    private val networkQueryContact: NetworkQueryContact,
    private val networkQueryMessage: NetworkQueryMessage,
    private val rsa: RSA,
    private val LOG: SphinxLogger,
): ChatRepository, ContactRepository, MessageRepository {

    companion object {
        const val TAG: String = "SphinxRepository"
    }

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
        return coreDB.getSphinxDatabaseQueries().chatGetAll()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { chatDboPresenterMapper.mapListFrom(it) }
    }

    override suspend fun getChatById(chatId: ChatId): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().chatGetById(chatId)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?> {
        return coreDB.getSphinxDatabaseQueries().chatGetByUUID(chatUUID)
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
                    emit(
                        processChatDtos(loadResponse.value)
                    )
                }
                is LoadResponse.Loading -> {
                    emit(loadResponse)
                }
            }

        }
    }

    private suspend fun processChatDtos(chats: List<ChatDto>): Response<Boolean, ResponseError> {
        try {
            val dbos = chatDtoDboMapper.mapListFrom(chats)

            val queries = coreDB.getSphinxDatabaseQueries()

            chatLock.withLock {
                withContext(dispatchers.io) {

                    val chatIdsToRemove = queries.chatGetAllIds()
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
                                LOG.d(TAG, "Removing Chats/Messages - chatId")
                                queries.chatDeleteById(chatId)
                                queries.messageDeleteByChatId(chatId)
                            }

                        }

                    }

                }

            }

            return Response.Success(true)

        } catch (e: IllegalArgumentException) {
            val msg = "Failed to convert Json from Relay while processing Chats"
            LOG.e(TAG, msg, e)
            return Response.Error(ResponseError(msg, e))
        } catch (e: ParseException) {
            val msg = "Failed to convert date/time from Relay while processing Chats"
            LOG.e(TAG, msg, e)
            return Response.Error(ResponseError(msg, e))
        }
    }


    ////////////////
    /// Contacts ///
    ////////////////
    private val contactLock = Mutex()
    private val contactDtoDboMapper: ContactDtoDboMapper by lazy {
        ContactDtoDboMapper(dispatchers)
    }
    private val contactDboPresenterMapper: ContactDboPresenterMapper by lazy {
        ContactDboPresenterMapper(dispatchers)
    }

    override suspend fun getContacts(): Flow<List<Contact>> {
        return coreDB.getSphinxDatabaseQueries().contactGetAll()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { contactDboPresenterMapper.mapListFrom(it) }
    }

    override suspend fun getContactById(contactId: ContactId): Flow<Contact?> {
        return coreDB.getSphinxDatabaseQueries().contactGetById(contactId)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getOwner(): Flow<Contact?> {
        return coreDB.getSphinxDatabaseQueries().contactGetOwner()
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
    }

    override fun networkRefreshContacts(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        networkQueryContact.getContacts().collect { loadResponse ->

            @Exhaustive
            when (loadResponse) {
                is Response.Error -> {
                    emit(loadResponse)
                }
                is Response.Success -> {

                    try {
                        val dbos = contactDtoDboMapper.mapListFrom(
                            loadResponse.value.contacts.filterNot {
                                it.from_group.toContactFromGroup().isTrue()
                            }
                        )

                        val queries = coreDB.getSphinxDatabaseQueries()

                        contactLock.withLock {
                            withContext(dispatchers.io) {

                                val contactIdsToRemove = queries.contactGetAllIds()
                                    .executeAsList()
                                    .toMutableSet()

                                queries.transaction {
                                    for (dbo in dbos) {
                                        queries.upsertContact(dbo)

                                        contactIdsToRemove.remove(dbo.id)
                                    }

                                    for (id in contactIdsToRemove) {
                                        queries.contactDeleteById(id)
                                    }
                                }

                            }
                        }

                        emit(
                            processChatDtos(loadResponse.value.chats)
                        )

                    } catch (e: ParseException) {
                        val msg = "Failed to convert date/time from Relay while processing Contacts"
                        LOG.e(TAG, msg, e)
                        emit(Response.Error(ResponseError(msg, e)))
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
    ): Response<UnencryptedByteArray, ResponseError> {
        val privateKey: CharArray = authenticationCoreManager.getEncryptionKey()
            ?.privateKey
            ?.value
            ?: return Response.Error(
                ResponseError("EncryptionKey retrieval failed")
            )

        return rsa.decrypt(
            rsaPrivateKey = RsaPrivateKey(privateKey),
            text = EncryptedString(messageContent.value),
            dispatcher = dispatchers.default
        )
    }

    @OptIn(UnencryptedDataAccess::class)
    private suspend fun mapMessageDboAndDecryptContentIfNeeded(
        queries: SphinxDatabaseQueries,
        messageDbo: MessageDbo
    ): Message {

        return messageDbo.message_content?.let { messageContent ->

            if (
                messageDbo.type !is MessageType.KeySend &&
                messageDbo.message_content_decrypted == null
            ) {

                val response = decryptMessageContent(messageContent)
                val message = messageDboPresenterMapper.mapFrom(messageDbo)

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        message.setDecryptionError(response.exception)
                        message
                    }
                    is Response.Success -> {

                        val decrypted = MessageContentDecrypted(
                            response.value.toUnencryptedString().value
                        )

                        messageLock.withLock {
                            withContext(dispatchers.io) {
                                queries.messageUpdateContentDecrypted(
                                    decrypted,
                                    messageDbo.id
                                )
                            }
                        }

                        message.setMessageContentDecrypted(decrypted)
                        message
                    }
                }

            } else {

                messageDboPresenterMapper.mapFrom(messageDbo)

            }

        } ?: messageDboPresenterMapper.mapFrom(messageDbo)
    }

    override suspend fun getMessageById(messageId: MessageId): Flow<Message?> {
        val queries = coreDB.getSphinxDatabaseQueries()
        return queries.messageGetById(messageId)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { messageDbo ->
                mapMessageDboAndDecryptContentIfNeeded(queries, messageDbo)
            }}
            .distinctUntilChanged()
    }

    override suspend fun getLatestMessageForChat(chatId: ChatId): Flow<Message?> {
        val queries = coreDB.getSphinxDatabaseQueries()
        return queries.messageGetLatestToShowByChatId(chatId)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { messageDbo ->
                mapMessageDboAndDecryptContentIfNeeded(queries, messageDbo)
            }}
            .distinctUntilChanged()
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
            queries.messageGetLatestId().executeAsOneOrNull()?.value?.let {
                if (it >= 1) {
                    it - 1
                } else {
                    0
                }
            } ?: 0
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

                            val jobList = ArrayList<Job>(newMessages.size)

                            for (message in newMessages) {

                                message.message_content?.let { content ->

                                    if (content.isNotEmpty() && message.type != MessageType.KEY_SEND) {

                                        scope.launch(dispatchers.mainImmediate) {
                                            val decrypted = decryptMessageContent(
                                                MessageContent(content)
                                            )

                                            @Exhaustive
                                            when (decrypted) {
                                                is Response.Error -> {}
                                                is Response.Success -> {
                                                    message.setMessageContentDecrypted(
                                                        decrypted.value.toUnencryptedString().value
                                                    )
                                                }
                                            }
                                        }.let { job ->
                                            jobList.add(job)
                                        }

                                    }

                                }
                            }

                            var count = 0
                            while (currentCoroutineContext().isActive) {
                                jobList.elementAtOrNull(count)?.join() ?: break
                                count++
                            }

                            val dbos = messageDtoDboMapper.mapListFrom(newMessages)

                            chatLock.withLock {
                                messageLock.withLock {
                                    withContext(dispatchers.io) {

                                        queries.transaction {
                                            val chatIds = queries.chatGetAllIds().executeAsList()
                                            LOG.d(
                                                TAG,
                                                "Inserting Messages -" +
                                                        " ${dbos.firstOrNull()?.id?.value}" +
                                                        " - ${dbos.lastOrNull()?.id?.value}"
                                            )

                                            val latestMessageMap = mutableMapOf<ChatId, MessageId>()

                                            for (dbo in dbos) {
                                                if (dbo.chat_id.value == MessageDtoDboMapper.NULL_CHAT_ID.toLong()) {
                                                    queries.upsertMessage(dbo)
                                                } else if (chatIds.contains(dbo.chat_id)) {
                                                    queries.upsertMessage(dbo)

                                                    if (dbo.type.show) {
                                                        latestMessageMap[dbo.chat_id] = dbo.id
                                                    }
                                                }
                                            }

                                            for (entry in latestMessageMap.entries) {
                                                queries.chatUpdateLatestMessage(entry.value, entry.key)
                                            }
                                        }

                                    }
                                }
                            }

                        }

                        when {
                            lastMessageId == -1 -> {}
                            newMessages.size >= 199 -> {
                                lastMessageId += 199
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
