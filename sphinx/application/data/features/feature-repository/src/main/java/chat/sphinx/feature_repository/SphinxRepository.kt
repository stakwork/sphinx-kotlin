package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.UpdateContactDto
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SphinxSocketIOMessageListener
import chat.sphinx.concept_socket_io.SphinxSocketIOMessage
import chat.sphinx.conceptcoredb.ChatDbo
import chat.sphinx.conceptcoredb.ContactDbo
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
import chat.sphinx.logger.w
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.Seen
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.DeviceId
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message.media.MediaKeyDecrypted
import chat.sphinx.wrapper_message.media.MessageMedia
import chat.sphinx.wrapper_rsa.RsaPrivateKey
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.annotations.UnencryptedDataAccess
import io.matthewnelson.crypto_common.clazzes.EncryptedString
import io.matthewnelson.crypto_common.clazzes.UnencryptedByteArray
import io.matthewnelson.crypto_common.clazzes.toUnencryptedString
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.ParseException
import java.util.*
import kotlin.collections.ArrayList

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
    private val socketIOManager: SocketIOManager,
    private val LOG: SphinxLogger,
) : ChatRepository,
    ContactRepository,
    LightningRepository,
    MessageRepository,
    SphinxSocketIOMessageListener
{

    companion object {
        const val TAG: String = "SphinxRepository"

        // PersistentStorage Keys
        const val REPOSITORY_LIGHTNING_BALANCE = "REPOSITORY_LIGHTNING_BALANCE"
        const val REPOSITORY_LAST_SEEN_MESSAGE_DATE = "REPOSITORY_LAST_SEEN_MESSAGE_DATE"
        const val REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE = "REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE"

        // networkRefreshMessages
        const val MESSAGE_PAGINATION_LIMIT = 200
        const val DATE_NIXON_SHOCK = "1971-08-15T00:00:00.000Z"
    }

    ////////////////
    /// SocketIO ///
    ////////////////
    init {
        socketIOManager.addListener(this)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun onSocketIOMessageReceived(msg: SphinxSocketIOMessage) {
        coreDB.getSphinxDatabaseQueriesOrNull()?.let { queries ->
            @Exhaustive
            when (msg) {
                is SphinxSocketIOMessage.Type.Contact -> {
                    contactLock.withLock {
                        queries.transaction {
                            queries.upsertContact(msg.dto)
                        }
                    }
                }
                is SphinxSocketIOMessage.Type.ChatSeen -> {
                    // TODO: Implement
                }
                is SphinxSocketIOMessage.Type.Group -> {
                    // TODO: Implement
                }
                is SphinxSocketIOMessage.Type.Invite -> {
                    // TODO: Implement
//                    queries.upsertInvite(msg.dto)
                }
                is SphinxSocketIOMessage.Type.InvoicePayment -> {
                    // TODO: Implement
                }
                is SphinxSocketIOMessage.Type.MessageType -> {

                    // TODO: Implement conditional arguments depending on
                    //  different MessageType

                    decryptMessageDtoContentIfAvailable(
                        msg.dto,
                        coroutineScope { this },
                        dispatchers.io
                    )?.join()

                    decryptMessageDtoMediaKeyIfAvailable(
                        msg.dto,
                        coroutineScope { this },
                        dispatchers.io
                    )?.join()

                    messageLock.withLock {
                        chatLock.withLock {
                            contactLock.withLock {
                                queries.transaction {

                                    queries.upsertMessage(msg.dto)

                                    var chatId: ChatId? = null

                                    msg.dto.chat?.let { chatDto ->
                                        queries.upsertChat(chatDto, moshi)

                                        chatId = ChatId(chatDto.id)
                                    }

                                    msg.dto.contact?.let { contactDto ->
                                        queries.upsertContact(contactDto)
                                    }

                                    msg.dto.chat_id?.let { nnChatId ->
                                        chatId = ChatId(nnChatId)
                                    }

                                    chatId?.let {  id ->
                                        if (msg.dto.updateChatDboLatestMessage){
                                            queries.chatUpdateLatestMessage(
                                                MessageId(msg.dto.id),
                                                id
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    /////////////
    /// Chats ///
    /////////////
    private val chatLock = Mutex()
    private val chatDboPresenterMapper: ChatDboPresenterMapper by lazy {
        ChatDboPresenterMapper(dispatchers)
    }

    override val getAllChats: Flow<List<Chat>> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().chatGetAll()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { chatDboPresenterMapper.mapListFrom(it) }
            )
        }
    }

    override fun getChatById(chatId: ChatId): Flow<Chat?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().chatGetById(chatId)
                .asFlow()
                .mapToOneOrNull(dispatchers.io)
                .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().chatGetByUUID(chatUUID)
                .asFlow()
                .mapToOneOrNull(dispatchers.io)
                .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getUnseenMessagesByChatId(chat: Chat): Flow<Long?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .chatGetUnseenIncomingMessagesCount(chat.contactIds.first(), chat.id)
                .asFlow()
                .mapToOneOrNull(dispatchers.io)
                .distinctUntilChanged()
        )
    }

    override val networkRefreshChats: Flow<LoadResponse<Boolean, ResponseError>> by lazy {
        flow {
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

    override val accountOwner: Flow<Contact?> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().contactGetOwner()
                    .asFlow()
                    .mapToOneOrNull(dispatchers.io)
                    .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
                    .distinctUntilChanged()
            )
        }
    }

    override val getAllContacts: Flow<List<Contact>> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().contactGetAll()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { contactDboPresenterMapper.mapListFrom(it) }
            )
        }
    }

    override fun getContactById(contactId: ContactId): Flow<Contact?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().contactGetById(contactId)
                .asFlow()
                .mapToOneOrNull(dispatchers.io)
                .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getInviteByContactId(contactId: ContactId): Flow<Invite?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().inviteGetByContactId(contactId)
                .asFlow()
                .mapToOneOrNull(dispatchers.io)
                .map { it?.let { inviteDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getInviteById(inviteId: InviteId): Flow<Invite?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().inviteGetById(inviteId)
                .asFlow()
                .mapToOneOrNull(dispatchers.io)
                .map { it?.let { inviteDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override val networkRefreshContacts: Flow<LoadResponse<Boolean, ResponseError>> by lazy {
        flow {
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
                            val msg =
                                "Failed to convert date/time from Relay while processing Contacts"
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
    }

    override suspend fun deleteContactById(contactId: ContactId): Response<Any, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()

        val owner: ContactDbo = queries.contactGetOwner().executeAsOneOrNull()
            ?: let {
                val msg = "Account Owner was not in the DB. Something is very wrong..."
                LOG.w(TAG, msg)
                return Response.Error(ResponseError(msg))
            }

        if (owner.id == contactId) {
            val msg = "deleteContactById called for account owner. Deletion not permitted."
            LOG.w(TAG, msg)
            return Response.Error(ResponseError(msg))
        }

        val response = networkQueryContact.deleteContact(contactId)

        if (response is Response.Success) {

            chatLock.withLock {
                messageLock.withLock {
                    contactLock.withLock {

                        val chat: ChatDbo? =
                            queries.chatGetConversationForContact(listOf(owner.id, contactId))
                                .executeAsOneOrNull()

                        queries.transaction {
                            chat?.let {
                                queries.messageDeleteByChatId(it.id)
                                queries.chatDeleteById(it.id)
                            }
                            queries.contactDeleteById(contactId)
                        }

                    }
                }
            }
        }

        return response
    }

    override suspend fun updateOwnerDeviceId(deviceId: DeviceId): Response<Any, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()
        var response: Response<Any, ResponseError> = Response.Success(Any())

        try {
            accountOwner.collect { owner ->

                if (owner != null) {

                    if (owner.deviceId != deviceId) {

                        networkQueryContact.updateContact(
                            owner.id,
                            UpdateContactDto(device_id = deviceId.value)
                        ).collect { loadResponse ->
                            @Exhaustive
                            when (loadResponse) {
                                is LoadResponse.Loading -> {}
                                is Response.Error -> {
                                    response = loadResponse
                                    throw Exception()
                                }
                                is Response.Success -> {
                                    contactLock.withLock {
                                        queries.upsertContact(loadResponse.value)
                                    }
                                    LOG.d(TAG, "DeviceId has been successfully updated")

                                    throw Exception()
                                }
                            }
                        }
                    } else {
                        LOG.d(TAG, "DeviceId is up to date")
                        throw Exception()
                    }

                }

            }
        } catch (e: Exception) {}

        return response
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

    override val networkRefreshBalance: Flow<LoadResponse<Boolean, ResponseError>> by lazy {
        flow {
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

    override suspend fun readMessages(chatId: ChatId) {
        val queries = coreDB.getSphinxDatabaseQueries()

        chatLock.withLock {
            messageLock.withLock {
                queries.transaction {
                    queries.chatUpdateSeen(Seen.True, chatId)
                    queries.chatMessagesUpdateSeen(Seen.True, chatId)
                }
            }
        }

        networkQueryMessage.readMessages(chatId).collect { _ -> }
    }

    @OptIn(UnencryptedDataAccess::class)
    override fun networkRefreshMessages(): Flow<LoadResponse<Boolean, ResponseError>> = flow {
        emit(LoadResponse.Loading)
        val queries = coreDB.getSphinxDatabaseQueries()

        val lastSeenMessagesDate: String? = authenticationStorage.getString(
            REPOSITORY_LAST_SEEN_MESSAGE_DATE,
            null
        )

        val page: Int = if (lastSeenMessagesDate == null) {
            authenticationStorage.getString(
                REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE,
                "0"
            )!!.toInt()
        } else {
            0
        }

        val lastSeenMessageDateResolved: DateTime = lastSeenMessagesDate?.toDateTime()
            ?: DATE_NIXON_SHOCK.toDateTime()

        val now: String = DateTime.getFormatRelay().format(Date(System.currentTimeMillis()))

        val supervisor = SupervisorJob(currentCoroutineContext().job)
        val scope = CoroutineScope(supervisor)

        var networkResponseError: Response.Error<ResponseError>? = null

        val jobList = ArrayList<Job>(MESSAGE_PAGINATION_LIMIT * 2 /* MessageDto fields to potentially decrypt */)

        var offset: Int = page * MESSAGE_PAGINATION_LIMIT
        while (currentCoroutineContext().isActive && offset >= 0) {

            networkQueryMessage.getMessages(
                MessagePagination.instantiate(
                    limit = MESSAGE_PAGINATION_LIMIT,
                    offset = offset,
                    date = lastSeenMessageDateResolved
                )
            ).collect { response ->

                @Exhaustive
                when (response) {
                    is LoadResponse.Loading -> {}

                    is Response.Error -> {

                        offset = -1
                        networkResponseError = response

                    }

                    is Response.Success -> {
                        val newMessages = response.value.new_messages

                        if (newMessages.isNotEmpty()) {

                            for (message in newMessages) {

                                decryptMessageDtoContentIfAvailable(message, scope)
                                    ?.let { jobList.add(it) }

                                decryptMessageDtoMediaKeyIfAvailable(message, scope)
                                    ?.let { jobList.add(it) }

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

                                                    if (dto.updateChatDboLatestMessage) {
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
                            offset == -1 -> {}
                            newMessages.size >= MESSAGE_PAGINATION_LIMIT -> {
                                offset += MESSAGE_PAGINATION_LIMIT

                                if (lastSeenMessagesDate == null) {
                                    val resumePageNumber = (offset / MESSAGE_PAGINATION_LIMIT)
                                    authenticationStorage.putString(
                                        REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE,
                                        resumePageNumber.toString()
                                    )
                                    LOG.d(
                                        TAG,
                                        "Persisting message restore page number: $resumePageNumber"
                                    )
                                }

                                jobList.clear()

                            }
                            else -> {
                                offset = -1
                            }
                        }
                    }
                }
            }
        }

        supervisor.cancelAndJoin()

        networkResponseError?.let { responseError ->

            emit(responseError)

        } ?: let {

            try {
                authenticationStorage.putString(
                    REPOSITORY_LAST_SEEN_MESSAGE_DATE,
                    now
                )
            } finally {
                if (lastSeenMessagesDate == null) {
                    authenticationStorage.removeString(REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE)
                    LOG.d(TAG, "Removing message restore page number")
                }
            }

            emit(Response.Success(true))
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    private suspend fun decryptMessageDtoContentIfAvailable(
        message: MessageDto,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = dispatchers.mainImmediate
    ): Job? =
        message.message_content?.let { content ->

            if (content.isNotEmpty() && message.type != MessageType.KEY_SEND) {

                scope.launch(dispatcher) {
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
                }

            } else {
                null
            }
        }

    @OptIn(UnencryptedDataAccess::class)
    private suspend fun decryptMessageDtoMediaKeyIfAvailable(
        message: MessageDto,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = dispatchers.mainImmediate,
    ): Job? =
        message.media_key?.let { mediaKey ->

            if (mediaKey.isNotEmpty()) {

                scope.launch(dispatcher) {

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

                }

            } else {
                null
            }
        }
}
