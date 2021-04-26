package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_coredb.util.upsertMessage
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.wrapper_rsa.RsaPrivateKey
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.conceptcoredb.MessageDbo
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.feature_repository.mappers.chat.ChatDboPresenterMapper
import chat.sphinx.feature_repository.mappers.contact.ContactDboPresenterMapper
import chat.sphinx.feature_repository.mappers.invite.InviteDboPresenterMapper
import chat.sphinx.feature_repository.mappers.mapListFrom
import chat.sphinx.feature_repository.mappers.message.MessageDboPresenterMapper
import chat.sphinx.feature_repository.util.*
import chat.sphinx.kotlin_response.*
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message.media.MediaKeyDecrypted
import chat.sphinx.wrapper_message.media.MessageMedia
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
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
    private val authenticationStorage: AuthenticationStorage,
    private val coreDB: CoreDB,
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkQueryChat: NetworkQueryChat,
    private val networkQueryContact: NetworkQueryContact,
    private val networkQueryLightning: NetworkQueryLightning,
    private val networkQueryMessage: NetworkQueryMessage,
    private val rsa: RSA,
    private val LOG: SphinxLogger,
): ChatRepository, ContactRepository, LightningRepository, MessageRepository {

    companion object {
        const val TAG: String = "SphinxRepository"

        const val REPOSITORY_LIGHTNING_BALANCE = "REPOSITORY_LIGHTNING_BALANCE"
    }

    /////////////
    /// Chats ///
    /////////////
    private val chatLock = Mutex()
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
            val queries = coreDB.getSphinxDatabaseQueries()

            chatLock.withLock {
                withContext(dispatchers.io) {

                    val chatIdsToRemove = queries.chatGetAllIds()
                        .executeAsList()
                        .toMutableSet()

                    messageLock.withLock {

                        queries.transaction {
                            for (dto in chats) {
                                queries.upsertChat(dto, moshi)

                                chatIdsToRemove.remove(ChatId(dto.id))
                            }

                            // remove remaining chat's from DB
                            for (chatId in chatIdsToRemove) {
                                LOG.d(TAG, "Removing Chats/Messages - chatId")
                                queries.chatDeleteById(chatId)
                                queries.messageDeleteByChatId(chatId)
                                queries.messageMediaDeleteByChatId(chatId)
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
    private val contactDboPresenterMapper: ContactDboPresenterMapper by lazy {
        ContactDboPresenterMapper(dispatchers)
    }
    private val inviteDboPresenterMapper: InviteDboPresenterMapper by lazy {
        InviteDboPresenterMapper(dispatchers)
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

    override suspend fun getInviteById(inviteId: InviteId): Flow<Invite?> {
        return coreDB.getSphinxDatabaseQueries().inviteGetById(inviteId)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { inviteDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getInviteByContactId(contactId: ContactId): Flow<Invite?> {
        return coreDB.getSphinxDatabaseQueries().inviteGetByContactId(contactId)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { inviteDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override suspend fun getOwner(): Flow<Contact?> {
        return coreDB.getSphinxDatabaseQueries().contactGetOwner()
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
    }

    override fun networkRefreshContacts(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        networkQueryContact.getContacts().collect { loadResponse ->

            @Exhaustive
            when (loadResponse) {
                is Response.Error -> {
                    emit(loadResponse)
                }
                is Response.Success -> {

                    val queries = coreDB.getSphinxDatabaseQueries()

                    try {
                        contactLock.withLock {
                            withContext(dispatchers.io) {

                                val contactIdsToRemove = queries.contactGetAllIds()
                                    .executeAsList()
                                    .toMutableSet()

                                queries.transaction {
                                    for (dto in loadResponse.value.contacts) {
                                        queries.upsertContact(dto)

                                        contactIdsToRemove.remove(ContactId(dto.id))
                                    }

                                    for (id in contactIdsToRemove) {
                                        queries.contactDeleteById(id)
                                        queries.inviteDeleteByContactId(id)
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

    /////////////////
    /// Lightning ///
    /////////////////
    @Suppress("RemoveExplicitTypeArguments")
    private val accountBalanceStateFlow: MutableStateFlow<NodeBalance?> by lazy {
        MutableStateFlow<NodeBalance?>(null)
    }
    private val balanceLock = Mutex()

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getAccountBalance(): StateFlow<NodeBalance?> {
        balanceLock.withLock {

            if (accountBalanceStateFlow.value == null) {
                authenticationStorage
                    .getString(REPOSITORY_LIGHTNING_BALANCE, null)
                    ?.let { balanceJsonString ->

                        val balanceDto: BalanceDto? = try {
                            withContext(dispatchers.default) {
                                moshi.adapter(BalanceDto::class.java)
                                    .fromJson(balanceJsonString)
                            }
                        } catch (e: Exception) {
                            null
                        }

                        balanceDto?.toNodeBalanceOrNull()?.let { nodeBalance ->
                            accountBalanceStateFlow.value = nodeBalance
                        }
                    }
            }

        }

        return accountBalanceStateFlow.asStateFlow()
    }

    override fun networkRefreshBalance(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        networkQueryLightning.getBalance().collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                    emit(loadResponse)
                }
                is Response.Error -> {
                    emit(loadResponse)
                }
                is Response.Success -> {

                    try {
                        val jsonString: String = withContext(dispatchers.default) {
                            moshi.adapter(BalanceDto::class.java)
                                .toJson(loadResponse.value)
                        } ?: throw NullPointerException("Converting BalanceDto to Json failed")

                        balanceLock.withLock {
                            accountBalanceStateFlow.value = loadResponse.value.toNodeBalance()

                            authenticationStorage.putString(
                                REPOSITORY_LIGHTNING_BALANCE,
                                jsonString
                            )
                        }

                        emit(Response.Success(true))
                    } catch (e: Exception) {

                        // this should _never_ happen, as if the network call was
                        // successful, it went from json -> dto, and we're just going
                        // back from dto -> json to persist it...
                        emit(
                            Response.Error(
                                ResponseError(
                                    """
                                        Network Fetching of balance was successful, but
                                        conversion to a string for persisting failed.
                                        ${loadResponse.value}
                                    """.trimIndent(),
                                    e
                                )
                            )
                        )
                    }

                }
            }
        }
    }


    ////////////////
    /// Messages ///
    ////////////////
    private val messageLock = Mutex()
    private val messageDboPresenterMapper: MessageDboPresenterMapper by lazy {
        MessageDboPresenterMapper(dispatchers, moshi)
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

        val message: Message = messageDbo.message_content?.let { messageContent ->

            if (
                messageDbo.type !is MessageType.KeySend &&
                messageDbo.message_content_decrypted == null
            ) {

                val response = decryptMessageContent(messageContent)

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        messageDboPresenterMapper.mapFrom(messageDbo).let { message ->
                            message.setDecryptionError(response.exception)
                            message
                        }
                    }
                    is Response.Success -> {

                        val decryptedContent = MessageContentDecrypted(
                            response.value.toUnencryptedString().value
                        )

                        messageLock.withLock {
                            withContext(dispatchers.io) {
                                queries.transaction {
                                    queries.messageUpdateContentDecrypted(
                                        decryptedContent,
                                        messageDbo.id
                                    )
                                }
                            }
                        }

                        messageDboPresenterMapper.mapFrom(messageDbo)
                            .setMessageContentDecrypted(decryptedContent)
                    }
                }

            } else {

                messageDboPresenterMapper.mapFrom(messageDbo)

            }
        } ?: messageDboPresenterMapper.mapFrom(messageDbo)

        if (message.type.canContainMedia) {
            withContext(dispatchers.io) {
                queries.messageMediaGetById(message.id).executeAsOneOrNull()
            }?.let { mediaDbo ->

                mediaDbo.media_key?.let { key ->

                    mediaDbo.media_key_decrypted.let { decrypted ->

                        if (decrypted == null) {
                            val response = decryptMessageContent(MessageContent(key.value))

                            @Exhaustive
                            when (response) {
                                is Response.Error -> {
                                    MessageMedia(
                                        mediaDbo.media_key,
                                        null,
                                        mediaDbo.media_type,
                                        mediaDbo.media_token
                                    ).also {
                                        it.setDecryptionError(response.exception)
                                        message.setMessageMedia(it)
                                    }
                                }
                                is Response.Success -> {
                                    val decryptedKey = MediaKeyDecrypted(
                                        response.value.toUnencryptedString().value
                                    )

                                    messageLock.withLock {
                                        withContext(dispatchers.io) {
                                            queries.messageMediaUpdateMediaKeyDecrypted(
                                                decryptedKey,
                                                mediaDbo.id
                                            )
                                        }
                                    }

                                    message.setMessageMedia(
                                        MessageMedia(
                                            mediaDbo.media_key,
                                            decryptedKey,
                                            mediaDbo.media_type,
                                            mediaDbo.media_token
                                        )
                                    )
                                }
                            }
                        } else {
                            message.setMessageMedia(
                                MessageMedia(
                                    mediaDbo.media_key,
                                    decrypted,
                                    mediaDbo.media_type,
                                    mediaDbo.media_token
                                )
                            )
                        }

                    }

                } ?: message.setMessageMedia(
                    MessageMedia(
                        mediaDbo.media_key,
                        mediaDbo.media_key_decrypted,
                        mediaDbo.media_type,
                        mediaDbo.media_token
                    )
                )

            } // else do nothing
        }

        return message
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

    override suspend fun getMessagesForChat(chatId: ChatId): Flow<List<Message>> {
        val queries = coreDB.getSphinxDatabaseQueries()
        return queries.messageGetAllToShowByChatId(chatId)
            .asFlow()
            .mapToList(dispatchers.io)
            .map { listMessageDbo ->
                listMessageDbo.map {
                    mapMessageDboAndDecryptContentIfNeeded(queries, it)
                }
            }
    }

    @OptIn(UnencryptedDataAccess::class)
    override fun networkRefreshMessages(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        emit(LoadResponse.Loading)
        val queries = coreDB.getSphinxDatabaseQueries()
        var lastMessageId: Long = withContext(dispatchers.io) {
            queries.messageGetLatestId().executeAsOneOrNull()?.value?.let {
                if (it >= 10) {
                    it - 10
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

                            val jobList = ArrayList<Job>(newMessages.size * 2)

                            for (message in newMessages) {

                                message.message_content?.let { content ->

                                    if (content.isNotEmpty() && message.type != MessageType.KEY_SEND) {

                                        scope.launch(dispatchers.mainImmediate) {
                                            val decrypted = decryptMessageContent(
                                                MessageContent(content)
                                            )

                                            @Exhaustive
                                            when (decrypted) {
                                                is Response.Error -> {
                                                    // Only log it if there is an exception
                                                    decrypted.exception?.let { nnE ->
                                                        LOG.e(
                                                            TAG,
                                                            """
                                                                ${decrypted.message}
                                                                MessageId: ${message.id}
                                                                MessageContent: ${message.message_content}
                                                            """.trimIndent(),
                                                            nnE
                                                        )
                                                    }
                                                }
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

                                message.media_key?.let { mediaKey ->

                                    if (mediaKey.isNotEmpty()) {

                                        scope.launch(dispatchers.mainImmediate) {

                                            val decrypted = decryptMessageContent(
                                                MessageContent(mediaKey)
                                            )

                                            @Exhaustive
                                            when (decrypted) {
                                                is Response.Error -> {
                                                    // Only log it if there is an exception
                                                    decrypted.exception?.let { nnE ->
                                                        LOG.e(
                                                            TAG,
                                                            """
                                                                ${decrypted.message}
                                                                MessageId: ${message.id}
                                                                MediaKey: ${message.media_key}
                                                            """.trimIndent(),
                                                            nnE
                                                        )
                                                    }
                                                }
                                                is Response.Success -> {
                                                    message.setMediaKeyDecrypted(
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

                            chatLock.withLock {
                                messageLock.withLock {
                                    withContext(dispatchers.io) {

                                        queries.transaction {
                                            val chatIds = queries.chatGetAllIds().executeAsList()
                                            LOG.d(
                                                TAG,
                                                "Inserting Messages -" +
                                                        " ${newMessages.firstOrNull()?.id}" +
                                                        " - ${newMessages.lastOrNull()?.id}"
                                            )

                                            val latestMessageMap = mutableMapOf<ChatId, MessageId>()

                                            for (dto in newMessages) {

                                                val id: Long? = dto.chat_id

                                                if (id == null) {
                                                    queries.upsertMessage(dto)
                                                } else if (chatIds.contains(ChatId(id))) {
                                                    queries.upsertMessage(dto)

                                                    if (
                                                        dto.type.toMessageType().show &&
                                                        dto.type != MessageType.BOT_RES
                                                    ) {
                                                        latestMessageMap[ChatId(id)] = MessageId(dto.id)
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
                            newMessages.size >= 190 -> {
                                lastMessageId += 190
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
