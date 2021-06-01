package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_chat.model.PutChatDto
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_contact.model.PostContactDto
import chat.sphinx.concept_network_query_contact.model.PutContactDto
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.concept_network_query_message.model.PostMessageDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard.RepositoryDashboard
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.SendMessage
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
import chat.sphinx.feature_repository.model.MessageDboWrapper
import chat.sphinx.feature_repository.util.*
import chat.sphinx.kotlin_response.*
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.logger.w
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message.media.MessageMedia
import chat.sphinx.wrapper_message.media.toMediaKeyDecrypted
import chat.sphinx.wrapper_rsa.RsaPrivateKey
import chat.sphinx.wrapper_rsa.RsaPublicKey
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
import io.matthewnelson.crypto_common.clazzes.UnencryptedString
import io.matthewnelson.crypto_common.clazzes.toUnencryptedString
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.ParseException
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

abstract class SphinxRepository(
    private val authenticationCoreManager: AuthenticationCoreManager,
    private val authenticationStorage: AuthenticationStorage,
    protected val coreDB: CoreDB,
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkQueryChat: NetworkQueryChat,
    private val networkQueryContact: NetworkQueryContact,
    private val networkQueryLightning: NetworkQueryLightning,
    private val networkQueryMessage: NetworkQueryMessage,
    private val rsa: RSA,
    private val socketIOManager: SocketIOManager,
    protected val LOG: SphinxLogger,
) : ChatRepository,
    ContactRepository,
    LightningRepository,
    MessageRepository,
    RepositoryDashboard,
    CoroutineDispatchers by dispatchers,
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

    private val supervisor = SupervisorJob()
    private val repositoryScope = CoroutineScope(supervisor)

    ////////////////
    /// SocketIO ///
    ////////////////
    init {
        socketIOManager.addListener(this)
    }

    override var updatedContactIds: MutableList<ContactId> = mutableListOf()

    /**
     * Call is made on [Dispatchers.IO]
     * */
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun onSocketIOMessageReceived(msg: SphinxSocketIOMessage) {
        coreDB.getSphinxDatabaseQueriesOrNull()?.let { queries ->
            @Exhaustive
            when (msg) {
                is SphinxSocketIOMessage.Type.Contact -> {
                    contactLock.withLock {
                        queries.transaction {
                            updatedContactIds.add(ContactId(msg.dto.id))
                            upsertContact(msg.dto, queries)
                        }
                    }
                }
                is SphinxSocketIOMessage.Type.ChatSeen -> {
                    readMessagesImpl(
                        chatId = ChatId(msg.dto.id),
                        queries = queries,
                        executeNetworkRequest = false
                    )
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
                        io
                    )?.join()

                    decryptMessageDtoMediaKeyIfAvailable(
                        msg.dto,
                        coroutineScope { this },
                        io
                    )?.join()

                    messageLock.withLock {
                        chatLock.withLock {
                            contactLock.withLock {
                                queries.transaction {

                                    upsertMessage(msg.dto, queries)

                                    var chatId: ChatId? = null

                                    msg.dto.contact?.let { contactDto ->
                                        upsertContact(contactDto, queries)
                                    }

                                    msg.dto.chat?.let { chatDto ->
                                        upsertChat(chatDto, moshi, chatSeenMap, queries, msg.dto.contact)

                                        chatId = ChatId(chatDto.id)
                                    }

                                    msg.dto.chat_id?.let { nnChatId ->
                                        chatId = ChatId(nnChatId)
                                    }

                                    chatId?.let { id ->
                                        updateChatDboLatestMessage(
                                            msg.dto,
                                            id,
                                            latestMessageUpdatedTimeMap,
                                            queries
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
                    .mapToList(io)
                    .map { chatDboPresenterMapper.mapListFrom(it) }
            )
        }
    }

    override fun getChatById(chatId: ChatId): Flow<Chat?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().chatGetById(chatId)
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getChatByUUID(chatUUID: ChatUUID): Flow<Chat?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().chatGetByUUID(chatUUID)
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getConversationByContactId(contactId: ContactId): Flow<Chat?> = flow {
        var ownerId: ContactId? = accountOwner.value?.id

        if (ownerId == null) {
            try {
                accountOwner.collect {
                    if (it != null) {
                        ownerId = it.id
                        delay(25L)
                        throw Exception()
                    } else {
                        emit(null)
                    }
                }
            } catch (e: Exception) {}
        }

        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .chatGetConversationForContact(listOf(ownerId!!, contactId))
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { chatDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getUnseenMessagesByChatId(chatId: ChatId): Flow<Long?> = flow {
        var ownerId: ContactId? = accountOwner.value?.id

        if (ownerId == null) {
            try {
                accountOwner.collect { contact ->
                    if (contact != null) {
                        ownerId = contact.id
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
            delay(25L)
        }

        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .messageGetUnseenIncomingMessageCountByChatId(ownerId!!, chatId)
                .asFlow()
                .mapToOneOrNull(io)
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

    private suspend fun processChatDtos(
        chats: List<ChatDto>,
        contacts: Map<ContactId, ContactDto>? = null
    ): Response<Boolean, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()
        try {

            var error: Throwable? = null
            val handler = CoroutineExceptionHandler { _, throwable ->
                error = throwable
            }

            repositoryScope.launch(io + handler) {
                chatLock.withLock {

                    val chatIdsToRemove = queries.chatGetAllIds()
                        .executeAsList()
                        .toMutableSet()

                    messageLock.withLock {

                        queries.transaction {
                            for (dto in chats) {

                                val contactDto: ContactDto? = if (dto.type == ChatType.CONVERSATION) {
                                    dto.contact_ids.elementAtOrNull(1)?.let { contactId ->
                                        contacts?.get(ContactId(contactId))
                                    }
                                } else {
                                    null
                                }

                                upsertChat(dto, moshi, chatSeenMap, queries, contactDto)

                                chatIdsToRemove.remove(ChatId(dto.id))
                            }

                            // remove remaining chat's from DB
                            for (chatId in chatIdsToRemove) {
                                LOG.d(TAG, "Removing Chats/Messages for $chatId")
                                deleteChatById(chatId, queries, latestMessageUpdatedTimeMap)
                            }

                        }

                    }

                }
            }.join()

            error?.let {
                throw it
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

    override val accountOwner: StateFlow<Contact?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().contactGetOwner()
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
        )
    }.stateIn(
        repositoryScope,
        SharingStarted.WhileSubscribed(5_000),
        null
    )

    override val getAllContacts: Flow<List<Contact>> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().contactGetAll()
                    .asFlow()
                    .mapToList(io)
                    .map { contactDboPresenterMapper.mapListFrom(it) }
            )
        }
    }

    override fun getContactById(contactId: ContactId): Flow<Contact?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().contactGetById(contactId)
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getInviteByContactId(contactId: ContactId): Flow<Invite?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().inviteGetByContactId(contactId)
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { inviteDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override fun getInviteById(inviteId: InviteId): Flow<Invite?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().inviteGetById(inviteId)
                .asFlow()
                .mapToOneOrNull(io)
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
                            var error: Throwable? = null
                            val handler = CoroutineExceptionHandler { _, throwable ->
                                error = throwable
                            }

                            var processChatsResponse: Response<Boolean, ResponseError> = Response.Success(true)

                            repositoryScope.launch(io + handler) {

                                val contactMap: MutableMap<ContactId, ContactDto> =
                                    LinkedHashMap(loadResponse.value.contacts.size)

                                contactLock.withLock {

                                    val contactIdsToRemove = queries.contactGetAllIds()
                                        .executeAsList()
                                        .toMutableSet()

                                    messageLock.withLock {
                                        chatLock.withLock {

                                            queries.transaction {
                                                for (dto in loadResponse.value.contacts) {

                                                    upsertContact(dto, queries)
                                                    contactMap[ContactId(dto.id)] = dto

                                                    contactIdsToRemove.remove(ContactId(dto.id))

                                                }

                                                for (contactId in contactIdsToRemove) {
                                                    deleteContactById(contactId, queries)
                                                }

                                            }

                                        }
                                    }

                                }

                                processChatsResponse = processChatDtos(
                                    loadResponse.value.chats,
                                    contactMap,
                                )
                            }.join()

                            error?.let {
                                throw it
                            }

                            emit(processChatsResponse)

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


        var owner: Contact? = accountOwner.value

        if (owner == null) {
            try {
                accountOwner.collect {
                    if (it != null) {
                        owner = it
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
            delay(25L)
        }

        if (owner?.id == null || owner!!.id == contactId) {
            val msg = "Account Owner was null, or deleteContactById was called for account owner."
            LOG.w(TAG, msg)
            return Response.Error(ResponseError(msg))
        }

        var deleteContactResponse: Response<Any, ResponseError> = Response.Success(Any())

        repositoryScope.launch(mainImmediate) {
            val response = networkQueryContact.deleteContact(contactId)
            deleteContactResponse = response

            if (response is Response.Success) {

                chatLock.withLock {
                    messageLock.withLock {
                        contactLock.withLock {

                            val chat: ChatDbo? =
                                queries.chatGetConversationForContact(listOf(owner!!.id, contactId))
                                    .executeAsOneOrNull()

                            queries.transaction {
                                deleteChatById(chat?.id, queries, latestMessageUpdatedTimeMap)
                                deleteContactById(contactId, queries)
                            }

                        }
                    }
                }
            }
        }.join()

        return deleteContactResponse
    }

    override fun createContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?,
    ): Flow<LoadResponse<Any, ResponseError>> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        val postContactDto = PostContactDto(
            alias = contactAlias.value,
            public_key = lightningNodePubKey.value,
            route_hint = lightningRouteHint?.value,
            status = ContactStatus.CONFIRMED.absoluteValue
        )

        val sharedFlow: MutableSharedFlow<Response<Boolean, ResponseError>> =
            MutableSharedFlow(1, 0)

        repositoryScope.launch(mainImmediate) {

            networkQueryContact.createContact(postContactDto).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading -> {}
                    is Response.Error -> {
                        sharedFlow.emit(loadResponse)
                    }
                    is Response.Success -> {
                        contactLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertContact(loadResponse.value, queries)
                                }
                            }
                        }

                        sharedFlow.emit(Response.Success(true))
                    }
                }
            }

        }

        emit(LoadResponse.Loading)

        sharedFlow.asSharedFlow().firstOrNull().let { response ->
            if (response == null) {
                emit(Response.Error(ResponseError("")))
            } else {
                emit(response)
            }
        }
    }

    override suspend fun updateOwner(
        alias: String?, privatePhoto: PrivatePhoto?, tipAmount: Sat?
    ): Response<Any, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()
        var response: Response<Any, ResponseError> = Response.Success(Any())

        try {
            accountOwner.collect { owner ->

                if (owner != null) {
                    networkQueryContact.updateContact(
                        owner.id,
                        PutContactDto(
                            alias = alias,
                            private_photo = privatePhoto?.isTrue(),
                            tip_amount = tipAmount?.value
                        )
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
                                    queries.transaction {
                                        upsertContact(loadResponse.value, queries)
                                    }
                                }
                                LOG.d(TAG, "Owner has been successfully updated")

                                throw Exception()
                            }
                        }
                    }
                }

            }
        } catch (e: Exception) {}

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
                            PutContactDto(device_id = deviceId.value)
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
                                        queries.transaction {
                                            upsertContact(loadResponse.value, queries)
                                        }
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
                            withContext(default) {
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
                            val jsonString: String = withContext(default) {
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
            dispatcher = default
        )
    }

    @OptIn(UnencryptedDataAccess::class)
    private suspend fun mapMessageDboAndDecryptContentIfNeeded(
        queries: SphinxDatabaseQueries,
        messageDbo: MessageDbo,
        reactions: List<Message>? = null,
    ): Message {

        val message: MessageDboWrapper = messageDbo.message_content?.let { messageContent ->

            if (
                messageDbo.type !is MessageType.KeySend &&
                messageDbo.message_content_decrypted == null
            ) {

                val response = decryptMessageContent(messageContent)

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        messageDboPresenterMapper.mapFrom(messageDbo).let { message ->
                            message._messageDecryptionException = response.exception
                            message._messageDecryptionError = true
                            message
                        }
                    }
                    is Response.Success -> {

                        val message: MessageDboWrapper = messageDboPresenterMapper.mapFrom(messageDbo)

                        response.value
                            .toUnencryptedString(trim = false)
                            .value
                            .toMessageContentDecrypted()
                            ?.let { decryptedContent ->

                                messageLock.withLock {

                                    withContext(io) {
                                        queries.transaction {
                                            queries.messageUpdateContentDecrypted(
                                                decryptedContent,
                                                messageDbo.id
                                            )
                                        }
                                    }

                                }

                                message._messageContentDecrypted = decryptedContent

                            } ?: message.also { it._messageDecryptionError = true }

                        message
                    }
                }

            } else {

                messageDboPresenterMapper.mapFrom(messageDbo)

            }
        } ?: messageDboPresenterMapper.mapFrom(messageDbo)

        if (message.type.canContainMedia) {
            withContext(io) {
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
                                        message._messageMedia = it
                                    }
                                }
                                is Response.Success -> {

                                    response.value
                                        .toUnencryptedString(trim = false)
                                        .value
                                        .toMediaKeyDecrypted()
                                        .let { decryptedKey ->

                                            message._messageMedia =
                                                MessageMedia(
                                                    mediaDbo.media_key,
                                                    decryptedKey,
                                                    mediaDbo.media_type,
                                                    mediaDbo.media_token
                                                ).also {

                                                    if (decryptedKey == null) {

                                                        it.setDecryptionError(null)

                                                    } else {

                                                        messageLock.withLock {

                                                            withContext(io) {
                                                                queries.messageMediaUpdateMediaKeyDecrypted(
                                                                    decryptedKey,
                                                                    mediaDbo.id
                                                                )
                                                            }

                                                        }

                                                    }
                                                }
                                        }
                                }
                            }
                        } else {
                            message._messageMedia =
                                MessageMedia(
                                    mediaDbo.media_key,
                                    decrypted,
                                    mediaDbo.media_type,
                                    mediaDbo.media_token
                                )
                        }

                    }

                } ?: message.also {
                    it._messageMedia =
                        MessageMedia(
                            mediaDbo.media_key,
                            mediaDbo.media_key_decrypted,
                            mediaDbo.media_type,
                            mediaDbo.media_token
                        )
                }

            } // else do nothing
        }

        message._reactions = reactions

        return message
    }

    override fun getAllMessagesToShowByChatId(chatId: ChatId): Flow<List<Message>> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()
        emitAll(
            queries.messageGetAllToShowByChatId(chatId)
                .asFlow()
                .mapToList(io)
                .map { listMessageDbo ->
                    withContext(default) {

                        val map: MutableMap<MessageUUID, ArrayList<Message>> =
                            LinkedHashMap(listMessageDbo.size)

                        for (dbo in listMessageDbo) {
                            dbo.uuid?.let {
                                map[it] = ArrayList(0)
                            }
                        }

                        val replyUUIDs = map.keys.map { ReplyUUID(it.value) }

                        replyUUIDs.chunked(500).forEach { chunkedIds ->
                            queries.messageGetAllReactionsByUUID(
                                chatId,
                                chunkedIds,
                            ).executeAsList()
                                .let { response ->
                                    response.forEach { dbo ->
                                        dbo.reply_uuid?.let { uuid ->
                                            map[MessageUUID(uuid.value)]?.add(
                                                mapMessageDboAndDecryptContentIfNeeded(queries, dbo)
                                            )
                                        }
                                    }
                                }
                        }

                        listMessageDbo.map { dbo ->
                            mapMessageDboAndDecryptContentIfNeeded(
                                queries,
                                dbo,
                                dbo.uuid?.let { map[it] }
                            )
                        }

                    }
                }
        )
    }

    override fun getMessageById(messageId: MessageId): Flow<Message?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()
        emitAll(
            queries.messageGetById(messageId)
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { messageDbo ->
                    mapMessageDboAndDecryptContentIfNeeded(queries, messageDbo)
                }}
                .distinctUntilChanged()
        )
    }

    @Suppress("RemoveExplicitTypeArguments")
    private val chatSeenMap: SynchronizedMap<ChatId, Seen> by lazy {
        SynchronizedMap<ChatId, Seen>()
    }

    override suspend fun readMessages(chatId: ChatId) {
        readMessagesImpl(
            chatId = chatId,
            queries = coreDB.getSphinxDatabaseQueries(),
            executeNetworkRequest = true
        )
    }

    private suspend fun readMessagesImpl(
        chatId: ChatId,
        queries: SphinxDatabaseQueries,
        executeNetworkRequest: Boolean
    ) {
        val wasMarkedSeen: Boolean =
            chatLock.withLock {
                messageLock.withLock {
                    withContext(io) {
                        chatSeenMap.withLock { map ->

                            if (map[chatId]?.isTrue() != true) {

                                queries.updateSeen(chatId)
                                LOG.d(TAG, "Chat [$chatId] marked as Seen")
                                map[chatId] = Seen.True

                                true
                            } else {
                                false
                            }
                        }
                    }
                }
        }

        if (executeNetworkRequest && wasMarkedSeen) {
            networkQueryMessage.readMessages(chatId).collect { _ -> }
        }
    }

    private val provisionalMessageLock = Mutex()

    // TODO: Rework to handle different message types
    @OptIn(RawPasswordAccess::class)
    override fun sendMessage(sendMessage: SendMessage?) {
        if (sendMessage == null) return

        repositoryScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()

            // TODO: Update SendMessage to accept a Chat && Contact instead of just IDs
            val chat: ChatDbo? = sendMessage.chatId?.let {
                withContext(io) {
                    queries.chatGetById(it).executeAsOneOrNull()
                }
            }

            val contact: ContactDbo? = sendMessage.contactId?.let {
                withContext(io) {
                    queries.contactGetById(it).executeAsOneOrNull()
                }
            }

            val owner: Contact? = accountOwner.value
                ?: let {
                    // TODO: Handle this better...
                    var owner: Contact? = null
                    try {
                        accountOwner.collect {
                            if (it != null) {
                                owner = it
                                throw Exception()
                            }
                        }
                    } catch (e: Exception) {}
                    delay(25L)
                    owner
                }

            if (owner == null) {
                LOG.w(TAG, "Owner returned null")
                return@launch
            }

            // encrypt text
            sendMessage.text?.let { msgText ->

                val selfEncrypted: MessageContent? = owner
                    .rsaPublicKey
                    ?.let { pubKey ->
                        val response = rsa.encrypt(
                            pubKey,
                            UnencryptedString(msgText),
                            formatOutput = false,
                            dispatcher = default,
                        )

                        @Exhaustive
                        when (response) {
                            is Response.Error -> {
                                LOG.e(TAG, response.message, response.exception)
                                null
                            }
                            is Response.Success -> {
                                MessageContent(response.value.value)
                            }
                        }
                    }

                if (selfEncrypted != null) {

                    val pricePerMessage = chat?.price_per_message?.value ?: 0
                    val escrowAmount = chat?.escrow_amount?.value ?: 0
                    val messagePrice = (pricePerMessage + escrowAmount).toSat() ?: Sat(0)

                    val provisionalMessageId: MessageId? = chat?.let { chatDbo ->
                        // Build provisional message and insert
                        provisionalMessageLock.withLock {
                            val currentProvisionalId: MessageId? = withContext(io) {
                                queries.messageGetLowestProvisionalMessageId().executeAsOneOrNull()
                            }

                            val provisionalId = MessageId((currentProvisionalId?.value ?: 0L) - 1)

                            withContext(io) {
                                queries.messageUpsert(
                                    MessageStatus.Pending,
                                    Seen.True,
                                    chatDbo.my_alias?.value?.toSenderAlias(),
                                    chatDbo.my_photo_url,
                                    null,
                                    null,
                                    provisionalId,
                                    null,
                                    chatDbo.id,
                                    MessageType.Message,
                                    owner.id,
                                    sendMessage.contactId,
                                    messagePrice,
                                    null,
                                    null,
                                    DateTime.nowUTC().toDateTime(),
                                    null,
                                    selfEncrypted,
                                    MessageContentDecrypted(msgText),
                                )
                            }

                            provisionalId
                        }
                    }

                    val remoteTextMap: Map<String, String>? = sendMessage.contactId?.let { nnContactId ->
                        // we know it's a conversation as the contactId is always sent
                        contact?.public_key?.let { pubKey ->
                            val response = rsa.encrypt(
                                pubKey,
                                UnencryptedString(msgText),
                                formatOutput = false,
                                dispatcher = default,
                            )

                            @Exhaustive
                            when (response) {
                                is Response.Error -> {
                                    LOG.e(TAG, response.message, response.exception)
                                    null
                                }
                                is Response.Success -> {
                                    mapOf(Pair(nnContactId.value.toString(), response.value.value))
                                }
                            }
                        }

                    } ?: chat?.group_key?.value?.let { rsaPubKeyString ->
                        val response = rsa.encrypt(
                            RsaPublicKey(rsaPubKeyString.toCharArray()),
                            UnencryptedString(msgText),
                            formatOutput = false,
                            dispatcher = default,
                        )

                        @Exhaustive
                        when (response) {
                            is Response.Error -> {
                                LOG.e(TAG, response.message, response.exception)
                                null
                            }
                            is Response.Success -> {
                                mapOf(Pair("chat", response.value.value))
                            }
                        }
                    }

                    remoteTextMap?.let { map ->
                        val postMessageDto: PostMessageDto? = try {
                            PostMessageDto(
                                sendMessage.chatId?.value,
                                sendMessage.contactId?.value,
                                messagePrice?.value ?: 0,
                                sendMessage.replyUUID?.value,
                                selfEncrypted.value,
                                map,
                            )
                        } catch (e: IllegalArgumentException) {
                            LOG.e(TAG, "Failed to create PostMessageDto", e)

                            provisionalMessageId?.let { provId ->
                                withContext(io) {
                                    queries
                                        .messageUpdateStatus(MessageStatus.Failed, provId)
                                }
                            }

                            null
                        }

                        if (postMessageDto != null) {
                            networkQueryMessage.sendMessage(postMessageDto).collect { loadResponse ->
                                @Exhaustive
                                when (loadResponse) {
                                    is LoadResponse.Loading -> {}
                                    is Response.Error -> {
                                        LOG.e(TAG, loadResponse.message, loadResponse.exception)

                                        provisionalMessageId?.let { provId ->
                                            withContext(io) {
                                                queries
                                                    .messageUpdateStatus(MessageStatus.Failed, provId)
                                            }
                                        }

                                    }
                                    is Response.Success -> {
                                        messageLock.withLock {
                                            chatLock.withLock {
                                                contactLock.withLock {
                                                    withContext(io) {

                                                        // chat is returned only if this is the
                                                        // first message sent to a new contact
                                                        loadResponse.value.chat?.let { chatDto ->
                                                            queries.transaction {
                                                                upsertChat(
                                                                    chatDto,
                                                                    moshi,
                                                                    chatSeenMap,
                                                                    queries,
                                                                    loadResponse.value.contact,
                                                                )
                                                            }
                                                        }

                                                        queries.transaction {

                                                            loadResponse.value.contact?.let { contactDto ->
                                                                upsertContact(contactDto, queries)
                                                            }

                                                            upsertMessage(loadResponse.value, queries)

                                                            provisionalMessageId?.let { provId ->
                                                                queries.messageDeleteById(provId)
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
                    }

                }
            }
        }
    }

    override suspend fun toggleChatMuted(chat: Chat): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError> = Response.Success(!chat.isMuted.isTrue())

        repositoryScope.launch(mainImmediate) {
            networkQueryChat.toggleMuteChat(chat.id, chat.isMuted).collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        val queries = coreDB.getSphinxDatabaseQueries()

                        chatLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    updateChatMuted(
                                        chat.id,
                                        loadResponse.value.isMutedActual.toChatMuted(),
                                        queries
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }.join()

        return response
    }

    override fun joinTribe(
        tribeDto: TribeDto,
    ): Flow<LoadResponse<Any, ResponseError>> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        val sharedFlow: MutableSharedFlow<Response<Boolean, ResponseError>> =
            MutableSharedFlow(1, 0)

        repositoryScope.launch(mainImmediate) {

            networkQueryChat.joinTribe(tribeDto).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading -> {}
                    is Response.Error -> {
                        sharedFlow.emit(loadResponse)
                    }
                    is Response.Success -> {
                        chatLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertChat(loadResponse.value, moshi, chatSeenMap, queries, null)
//                                    updateChatTribeData(tribeDto, ChatId(loadResponse.value.id), queries)
                                }
                            }
                        }

                        sharedFlow.emit(Response.Success(true))
                    }
                }
            }

        }

        emit(LoadResponse.Loading)

        sharedFlow.asSharedFlow().firstOrNull().let { response ->
            if (response == null) {
                emit(Response.Error(ResponseError("")))
            } else {
                emit(response)
            }
        }
    }

    override suspend fun updateTribeInfo(chat: Chat) {

        chat.host?.let { chatHost ->
            val chatUUID = chat.uuid

            if (chat.isTribe() &&
                chatHost.toString().isNotEmpty() &&
                chatUUID.toString().isNotEmpty()
            ) {

                val queries = coreDB.getSphinxDatabaseQueries()

                networkQueryChat.getTribeInfo(chatHost, chatUUID).collect { loadResponse ->
                    when (loadResponse) {

                        is LoadResponse.Loading -> {}
                        is Response.Error -> {}

                        is Response.Success -> {
                            val tribeDto = loadResponse.value

                            val didChangeNameOrPhotoUrl = (
                                tribeDto.name != chat.name?.value ?: "" ||
                                tribeDto.img != chat.photoUrl?.value ?: ""
                            )

                            chatLock.withLock {
                                queries.transaction {
                                    updateChatTribeData(tribeDto, chat.id, queries)
                                }
                            }

                            if (didChangeNameOrPhotoUrl) {
                                networkQueryChat.updateChat(chat.id,
                                    PutChatDto(
                                        tribeDto.name,
                                        tribeDto.img ?: "",
                                    )
                                ).collect {}
                            }
                        }
                    }
                }
            }

        }
    }

    /*
    * Used to hold in memory the chat table's latest message time to reduce disk IO
    * and mitigate conflicting updates between SocketIO and networkRefreshMessages
    * */
    @Suppress("RemoveExplicitTypeArguments")
    private val latestMessageUpdatedTimeMap: SynchronizedMap<ChatId, DateTime> by lazy {
        SynchronizedMap<ChatId, DateTime>()
    }

    override val networkRefreshMessages: Flow<LoadResponse<Boolean, ResponseError>> by lazy {
        flow {
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

            val now: String = DateTime.nowUTC()

            val supervisor = SupervisorJob(currentCoroutineContext().job)
            val scope = CoroutineScope(supervisor)

            var networkResponseError: Response.Error<ResponseError>? = null

            val jobList =
                ArrayList<Job>(MESSAGE_PAGINATION_LIMIT * 2 /* MessageDto fields to potentially decrypt */)

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
                        is LoadResponse.Loading -> {
                        }

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

                                repositoryScope.launch(io) {

                                    chatLock.withLock {
                                        messageLock.withLock {

                                            queries.transaction {
                                                val chatIds =
                                                    queries.chatGetAllIds().executeAsList()
                                                LOG.d(
                                                    TAG,
                                                    "Inserting Messages -" +
                                                            " ${newMessages.firstOrNull()?.id}" +
                                                            " - ${newMessages.lastOrNull()?.id}"
                                                )

                                                val latestMessageMap =
                                                    mutableMapOf<ChatId, MessageDto>()

                                                for (dto in newMessages) {

                                                    val id: Long? = dto.chat_id

                                                    if (id == null) {
                                                        upsertMessage(dto, queries)
                                                    } else if (chatIds.contains(ChatId(id))) {
                                                        upsertMessage(dto, queries)

                                                        if (dto.updateChatDboLatestMessage) {
                                                            latestMessageMap[ChatId(id)] = dto
                                                        }
                                                    }
                                                }

                                                latestMessageUpdatedTimeMap.withLock { map ->

                                                    for (entry in latestMessageMap.entries) {

                                                        updateChatDboLatestMessage(
                                                            entry.value,
                                                            entry.key,
                                                            map,
                                                            queries
                                                        )

                                                    }

                                                }
                                            }

                                        }
                                    }
                                }.join()

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

            } ?: repositoryScope.launch(mainImmediate) {

                authenticationStorage.putString(
                    REPOSITORY_LAST_SEEN_MESSAGE_DATE,
                    now
                )

                if (lastSeenMessagesDate == null) {
                    authenticationStorage.removeString(REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE)
                    LOG.d(TAG, "Removing message restore page number")
                }

            }.join()

            emit(Response.Success(true))
        }
    }

    @OptIn(UnencryptedDataAccess::class)
    private suspend fun decryptMessageDtoContentIfAvailable(
        message: MessageDto,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = mainImmediate
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
                                decrypted.value.toUnencryptedString(trim = false).value
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
        dispatcher: CoroutineDispatcher = mainImmediate,
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
                                decrypted.value.toUnencryptedString(trim = false).value
                            )
                        }
                    }

                }

            } else {
                null
            }
        }
}
