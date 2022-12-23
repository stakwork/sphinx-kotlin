package chat.sphinx.feature_repository

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.concept_network_query_action_track.NetworkQueryActionTrack
import chat.sphinx.concept_network_query_action_track.model.ActionTrackDto
import chat.sphinx.concept_network_query_action_track.model.ActionTrackMetaDataDto
import chat.sphinx.concept_network_query_action_track.model.SyncActionsDto
import chat.sphinx.concept_network_query_action_track.model.toActionTrackMetaDataDtoOrNull
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.*
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_contact.model.GithubPATDto
import chat.sphinx.concept_network_query_contact.model.PostContactDto
import chat.sphinx.concept_network_query_contact.model.PutContactDto
import chat.sphinx.concept_network_query_feed_search.NetworkQueryFeedSearch
import chat.sphinx.concept_network_query_feed_search.model.toFeedSearchResult
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_meme_server.NetworkQueryMemeServer
import chat.sphinx.concept_network_query_meme_server.model.PostMemeServerUploadDto
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_message.model.*
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_network_query_redeem_badge_token.NetworkQueryRedeemBadgeToken
import chat.sphinx.concept_network_query_people.model.DeletePeopleProfileDto
import chat.sphinx.concept_network_query_people.model.PeopleProfileDto
import chat.sphinx.concept_network_query_redeem_badge_token.model.RedeemBadgeTokenDto
import chat.sphinx.concept_network_query_relay_keys.NetworkQueryRelayKeys
import chat.sphinx.concept_network_query_relay_keys.model.PostHMacKeyDto
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_subscription.model.PostSubscriptionDto
import chat.sphinx.concept_network_query_subscription.model.PutSubscriptionDto
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.concept_network_query_verify_external.NetworkQueryAuthorizeExternal
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_chat.model.AddMember
import chat.sphinx.concept_repository_chat.model.CreateTribe
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_dashboard.RepositoryDashboard
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.model.AttachmentInfo
import chat.sphinx.concept_repository_message.model.SendMessage
import chat.sphinx.concept_repository_message.model.SendPayment
import chat.sphinx.concept_repository_message.model.SendPaymentRequest
import chat.sphinx.concept_repository_subscription.SubscriptionRepository
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SphinxSocketIOMessage
import chat.sphinx.concept_socket_io.SphinxSocketIOMessageListener
import chat.sphinx.conceptcoredb.*
import chat.sphinx.feature_repository.mappers.action_track.ActionTrackDboContentBoostPresenterMapper
import chat.sphinx.feature_repository.mappers.action_track.ActionTrackDboContentConsumedPresenterMapper
import chat.sphinx.feature_repository.mappers.action_track.ActionTrackDboMessagePresenterMapper
import chat.sphinx.feature_repository.mappers.action_track.ActionTrackDboPodcastClipCommentPresenterMapper
import chat.sphinx.feature_repository.mappers.action_track.ActionTrackDboFeedSearchPresenterMapper
import chat.sphinx.feature_repository.mappers.chat.ChatDboPresenterMapper
import chat.sphinx.feature_repository.mappers.contact.ContactDboPresenterMapper
import chat.sphinx.feature_repository.mappers.feed.FeedDboPresenterMapper
import chat.sphinx.feature_repository.mappers.feed.FeedDestinationDboPresenterMapper
import chat.sphinx.feature_repository.mappers.feed.FeedItemDboPresenterMapper
import chat.sphinx.feature_repository.mappers.feed.FeedModelDboPresenterMapper
import chat.sphinx.feature_repository.mappers.feed.podcast.*
import chat.sphinx.feature_repository.mappers.invite.InviteDboPresenterMapper
import chat.sphinx.feature_repository.mappers.mapListFrom
import chat.sphinx.feature_repository.mappers.message.MessageDboPresenterMapper
import chat.sphinx.feature_repository.mappers.subscription.SubscriptionDboPresenterMapper
import chat.sphinx.feature_repository.model.message.MessageDboWrapper
import chat.sphinx.feature_repository.model.message.MessageMediaDboWrapper
import chat.sphinx.feature_repository.util.*
import chat.sphinx.kotlin_response.*
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.logger.w
import chat.sphinx.notification.SphinxNotificationManager
import chat.sphinx.wrapper_action_track.*
import chat.sphinx.wrapper_action_track.action_wrappers.*
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.contact.Blocked
import chat.sphinx.wrapper_common.contact.isTrue
import chat.sphinx.wrapper_common.dashboard.*
import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_common.invite.InviteStatus
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.message.*
import chat.sphinx.wrapper_common.payment.PaymentTemplate
import chat.sphinx.wrapper_common.subscription.EndNumber
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_invite.Invite
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_lightning.NodeBalanceAll
import chat.sphinx.wrapper_meme_server.AuthenticationChallenge
import chat.sphinx.wrapper_meme_server.PublicAttachmentInfo
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message_media.*
import chat.sphinx.wrapper_message_media.token.MediaHost
import chat.sphinx.wrapper_podcast.FeedRecommendation
import chat.sphinx.wrapper_podcast.FeedSearchResultRow
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import chat.sphinx.wrapper_relay.*
import chat.sphinx.wrapper_rsa.RsaPrivateKey
import chat.sphinx.wrapper_rsa.RsaPublicKey
import chat.sphinx.wrapper_subscription.Subscription
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.annotations.UnencryptedDataAccess
import io.matthewnelson.crypto_common.clazzes.*
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.base64.encodeBase64
import java.io.File
import java.io.InputStream
import java.text.ParseException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.math.absoluteValue


abstract class SphinxRepository(
    override val accountOwner: StateFlow<Contact?>,
    private val applicationScope: CoroutineScope,
    private val authenticationCoreManager: AuthenticationCoreManager,
    private val authenticationStorage: AuthenticationStorage,
    private val relayDataHandler: RelayDataHandler,
    protected val coreDB: CoreDB,
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val mediaCacheHandler: MediaCacheHandler,
    private val memeInputStreamHandler: MemeInputStreamHandler,
    private val memeServerTokenHandler: MemeServerTokenHandler,
    private val networkQueryActionTrack: NetworkQueryActionTrack,
    private val networkQueryMemeServer: NetworkQueryMemeServer,
    private val networkQueryChat: NetworkQueryChat,
    private val networkQueryContact: NetworkQueryContact,
    private val networkQueryLightning: NetworkQueryLightning,
    private val networkQueryMessage: NetworkQueryMessage,
    private val networkQueryInvite: NetworkQueryInvite,
    private val networkQueryAuthorizeExternal: NetworkQueryAuthorizeExternal,
    private val networkQueryPeople: NetworkQueryPeople,
    private val networkQueryRedeemBadgeToken: NetworkQueryRedeemBadgeToken,
    private val networkQuerySubscription: NetworkQuerySubscription,
    private val networkQueryFeedSearch: NetworkQueryFeedSearch,
    private val networkQueryRelayKeys: NetworkQueryRelayKeys,
    private val rsa: RSA,
    private val socketIOManager: SocketIOManager,
    private val sphinxNotificationManager: SphinxNotificationManager,
    private val LOG: SphinxLogger,
) : ChatRepository,
    ContactRepository,
    LightningRepository,
    MessageRepository,
    SubscriptionRepository,
    RepositoryDashboard,
    RepositoryMedia,
    ActionsRepository,
    FeedRepository,
    CoroutineDispatchers by dispatchers,
    SphinxSocketIOMessageListener {

    companion object {
        const val TAG: String = "SphinxRepository"

        // PersistentStorage Keys
        const val REPOSITORY_LIGHTNING_BALANCE = "REPOSITORY_LIGHTNING_BALANCE"
        const val REPOSITORY_LAST_SEEN_MESSAGE_DATE = "REPOSITORY_LAST_SEEN_MESSAGE_DATE"
        const val REPOSITORY_LAST_SEEN_CONTACTS_DATE = "REPOSITORY_LAST_SEEN_CONTACTS_DATE"
        const val REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE =
            "REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE"

        // networkRefreshMessages
        const val MESSAGE_PAGINATION_LIMIT = 200
        const val DATE_NIXON_SHOCK = "1971-08-15T00:00:00.000Z"

        const val MEDIA_KEY_SIZE = 32
        const val MEDIA_PROVISIONAL_TOKEN = "Media_Provisional_Token"

        const val AUTHORIZE_EXTERNAL_BASE_64 = "U3BoaW54IFZlcmlmaWNhdGlvbg=="
    }

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
                is SphinxSocketIOMessage.Type.Invite -> {
                    contactLock.withLock {
                        queries.transaction {
                            updatedContactIds.add(ContactId(msg.dto.contact_id))
                            upsertInvite(msg.dto, queries)
                        }
                    }
                }
                is SphinxSocketIOMessage.Type.InvoicePayment -> {
                    // TODO: Implement
                }
                is SphinxSocketIOMessage.Type.MessageType, is SphinxSocketIOMessage.Type.Group -> {

                    val messageDto: MessageDto? = when (msg) {
                        is SphinxSocketIOMessage.Type.MessageType -> msg.dto
                        is SphinxSocketIOMessage.Type.Group -> msg.dto.message
                        else -> null
                    }

                    val contactDto: ContactDto? = when (msg) {
                        is SphinxSocketIOMessage.Type.MessageType -> msg.dto.contact
                        is SphinxSocketIOMessage.Type.Group -> msg.dto.contact
                        else -> null
                    }

                    val chatDto: ChatDto? = when (msg) {
                        is SphinxSocketIOMessage.Type.MessageType -> msg.dto.chat
                        is SphinxSocketIOMessage.Type.Group -> msg.dto.chat
                        else -> null
                    }

                    val chatDtoId: ChatId? = when (msg) {
                        is SphinxSocketIOMessage.Type.MessageType -> msg.dto.chat_id?.toChatId()
                        else -> null
                    }

                    messageDto?.let { nnMessageDto ->
                        decryptMessageDtoContentIfAvailable(
                            nnMessageDto,
                            coroutineScope { this },
                            io
                        )?.join()

                        decryptMessageDtoMediaKeyIfAvailable(
                            nnMessageDto,
                            coroutineScope { this },
                            io
                        )?.join()

                        val isAttachmentMessage = nnMessageDto.type.toMessageType().isAttachment()
                        delay(if (isAttachmentMessage) 500L else 0L)

                        chatLock.withLock {
                            messageLock.withLock {
                                contactLock.withLock {
                                    queries.transaction {

                                        upsertMessage(nnMessageDto, queries)

                                        var chatId: ChatId? = null

                                        contactDto?.let { nnContactDto ->
                                            upsertContact(nnContactDto, queries)
                                        }

                                        chatDto?.let { nnChatDto ->
                                            upsertChat(
                                                nnChatDto,
                                                moshi,
                                                chatSeenMap,
                                                queries,
                                                contactDto,
                                                accountOwner.value?.nodePubKey
                                            )

                                            chatId = ChatId(nnChatDto.id)
                                        }

                                        chatDtoId?.let { nnChatDtoId ->
                                            chatId = nnChatDtoId
                                        }

                                        chatId?.let { id ->
                                            updateChatDboLatestMessage(
                                                nnMessageDto,
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

    override val getAllContactChats: Flow<List<Chat>> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().chatGetAllContact()
                    .asFlow()
                    .mapToList(io)
                    .map { chatDboPresenterMapper.mapListFrom(it) }
            )
        }
    }

    override val getAllTribeChats: Flow<List<Chat>> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().chatGetAllTribe()
                    .asFlow()
                    .mapToList(io)
                    .map { chatDboPresenterMapper.mapListFrom(it) }
            )
        }
    }

    override suspend fun getAllChatsByIds(chatIds: List<ChatId>): List<Chat> {
        return coreDB.getSphinxDatabaseQueries()
            .chatGetAllByIds(chatIds)
            .executeAsList()
            .map { chatDboPresenterMapper.mapFrom(it) }
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
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
            }
            delay(25L)
        }

        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .chatGetConversationForContact(
                    if (ownerId != null) {
                        listOf(ownerId!!, contactId)
                    } else {
                        listOf()
                    }
                )
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
            } catch (e: Exception) {
            }
            delay(25L)
        }

        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .messageGetUnseenIncomingMessageCountByChatId(
                    ownerId ?: ContactId(-1),
                    chatId
                )
                .asFlow()
                .mapToOneOrNull(io)
                .distinctUntilChanged()
        )
    }

    override fun getUnseenMentionsByChatId(chatId: ChatId): Flow<Long?> = flow {
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
                .messageGetUnseenIncomingMentionsCountByChatId(
                    ownerId ?: ContactId(-1),
                    chatId
                )
                .asFlow()
                .mapToOneOrNull(io)
                .distinctUntilChanged()
        )
    }

    override fun getUnseenActiveConversationMessagesCount(): Flow<Long?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()
        var ownerId: ContactId? = accountOwner.value?.id

        if (ownerId == null) {
            try {
                accountOwner.collect { contact ->
                    if (contact != null) {
                        ownerId = contact.id
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
            }
            delay(25L)
        }

        val blockedContactIds = queries.contactGetBlocked().executeAsList().map { it.id }

        emitAll(
            queries
                .messageGetUnseenIncomingMessageCountByChatType(
                    ownerId ?: ContactId(-1),
                    blockedContactIds,
                    ChatType.Conversation
                )
                .asFlow()
                .mapToOneOrNull(io)
                .distinctUntilChanged()
        )
    }

    override fun getUnseenTribeMessagesCount(): Flow<Long?> = flow {
        var ownerId: ContactId? = accountOwner.value?.id

        if (ownerId == null) {
            try {
                accountOwner.collect { contact ->
                    if (contact != null) {
                        ownerId = contact.id
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
            }
            delay(25L)
        }

        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .messageGetUnseenIncomingMessageCountByChatType(
                    ownerId ?: ContactId(-1),
                    listOf(),
                    ChatType.Tribe
                )
                .asFlow()
                .mapToOneOrNull(io)
                .distinctUntilChanged()
        )
    }

    override fun getPaymentsTotalFor(feedId: FeedId): Flow<Sat?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .messageGetAmountSumForMessagesStartingWith(
                    "{\"feedID\":${feedId.value.toLongOrNull()}%",
                    "{\"feedID\":\"${feedId.value}\"%"
                )
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.SUM }
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
                        emit(processChatDtos(loadResponse.value))
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

            applicationScope.launch(io + handler) {
                chatLock.withLock {

                    messageLock.withLock {

                        queries.transaction {
                            for (dto in chats) {
                                if (dto.deletedActual) {
                                    LOG.d(TAG, "Removing Chats/Messages for ${ChatId(dto.id)}")
                                    deleteChatById(
                                        ChatId(dto.id),
                                        queries,
                                        latestMessageUpdatedTimeMap
                                    )
                                } else {
                                    val contactDto: ContactDto? =
                                        if (dto.type == ChatType.CONVERSATION) {
                                            dto.contact_ids.elementAtOrNull(1)?.let { contactId ->
                                                contacts?.get(ContactId(contactId))
                                            }
                                        } else {
                                            null
                                        }

                                    upsertChat(
                                        dto,
                                        moshi,
                                        chatSeenMap,
                                        queries,
                                        contactDto,
                                        accountOwner.value?.nodePubKey
                                    )
                                }

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

    override fun updateChatMetaData(
        chatId: ChatId,
        podcastId: FeedId?,
        metaData: ChatMetaData,
        shouldSync: Boolean
    ) {
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            if (podcastId?.value == FeedRecommendation.RECOMMENDATION_PODCAST_ID) {
                return@launch
            }

            if (chatId.value == ChatId.NULL_CHAT_ID.toLong()) {
                //Podcast with no chat. Updating current item id
                podcastId?.let { nnPodcastId ->
                    podcastLock.withLock {
                        queries.feedUpdateCurrentItemId(
                            metaData.itemId,
                            nnPodcastId
                        )
                    }
                }
                return@launch
            }

            chatLock.withLock {
                queries.chatUpdateMetaData(metaData, chatId)
            }

            podcastLock.withLock {
                queries.feedUpdateCurrentItemIdByChatId(
                    metaData.itemId,
                    chatId
                )
            }

            if (shouldSync) {
                try {
                    networkQueryChat.updateChat(
                        chatId,
                        PutChatDto(meta = metaData.toJson(moshi))
                    ).collect {}
                } catch (e: AssertionError) {
                }
            }
        }
    }

    override fun streamFeedPayments(
        chatId: ChatId,
        metaData: ChatMetaData,
        podcastId: String,
        episodeId: String,
        destinations: List<FeedDestination>,
        updateMetaData: Boolean,
        clipMessageUUID: MessageUUID?,
    ) {

        if (chatId.value == ChatId.NULL_CHAT_ID.toLong()) {
            return
        }

        if (metaData.satsPerMinute.value <= 0 || destinations.isEmpty()) {
            return
        }

        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            chatLock.withLock {
                queries.chatUpdateMetaData(metaData, chatId)
            }

            val destinationsArray: MutableList<PostStreamSatsDestinationDto> =
                ArrayList(destinations.size)

            for (destination in destinations) {
                destinationsArray.add(
                    PostStreamSatsDestinationDto(
                        destination.address.value,
                        destination.type.value,
                        destination.split.value,
                    )
                )
            }

            val streamSatsText =
                StreamSatsText(
                    podcastId,
                    episodeId,
                    metaData.timeSeconds.toLong(),
                    metaData.speed,
                    clipMessageUUID?.value
                )

            val postStreamSatsDto = PostStreamSatsDto(
                metaData.satsPerMinute.value,
                chatId.value,
                streamSatsText.toJson(moshi),
                updateMetaData,
                destinationsArray
            )

            try {
                networkQueryChat.streamSats(
                    postStreamSatsDto
                ).collect {}
            } catch (e: AssertionError) {
            }
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

    override val getAllNotBlockedContacts: Flow<List<Contact>> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().contactGetNotBlocked()
                    .asFlow()
                    .mapToList(io)
                    .map { contactDboPresenterMapper.mapListFrom(it) }
            )
        }
    }

    override val getAllInvites: Flow<List<Invite>> by lazy {
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries().inviteGetAll()
                    .asFlow()
                    .mapToList(io)
                    .map { inviteDboPresenterMapper.mapListFrom(it) }
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

    override fun getContactByPubKey(pubKey: LightningNodePubKey): Flow<Contact?> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries().contactGetByPubKey(pubKey)
                .asFlow()
                .mapToOneOrNull(io)
                .map { it?.let { contactDboPresenterMapper.mapFrom(it) } }
                .distinctUntilChanged()
        )
    }

    override suspend fun getAllContactsByIds(contactIds: List<ContactId>): List<Contact> {
        return coreDB.getSphinxDatabaseQueries()
            .contactGetAllByIds(contactIds)
            .executeAsList()
            .map { contactDboPresenterMapper.mapFrom(it) }
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

                            var processChatsResponse: Response<Boolean, ResponseError> =
                                Response.Success(true)

                            applicationScope.launch(io + handler) {

                                val contactMap: MutableMap<ContactId, ContactDto> =
                                    LinkedHashMap(loadResponse.value.contacts.size)

                                chatLock.withLock {
                                    messageLock.withLock {
                                        contactLock.withLock {

                                            val contactIdsToRemove = queries.contactGetAllIds()
                                                .executeAsList()
                                                .toMutableSet()

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

    private val inviteLock = Mutex()
    override val networkRefreshLatestContacts: Flow<LoadResponse<RestoreProgress, ResponseError>> by lazy {
        flow {

            val lastSeenContactsDate: String? = authenticationStorage.getString(
                REPOSITORY_LAST_SEEN_CONTACTS_DATE,
                null
            )

            val lastSeenContactsDateResolved: DateTime = lastSeenContactsDate?.toDateTime()
                ?: DATE_NIXON_SHOCK.toDateTime()

            val now: String = DateTime.nowUTC()
            val restoring = lastSeenContactsDate == null

            emit(
                Response.Success(
                    RestoreProgress(restoring, 2)
                )
            )

            networkQueryContact.getLatestContacts(
                lastSeenContactsDateResolved
            ).collect { loadResponse ->

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

                            var processChatsResponse: Response<Boolean, ResponseError> =
                                Response.Success(true)

                            applicationScope.launch(io + handler) {

                                val contactsToInsert =
                                    loadResponse.value.contacts.filter { dto -> !dto.deletedActual && !dto.fromGroupActual }
                                val contactMap: MutableMap<ContactId, ContactDto> =
                                    LinkedHashMap(contactsToInsert.size)

                                chatLock.withLock {
                                    messageLock.withLock {
                                        contactLock.withLock {
                                            queries.transaction {
                                                for (dto in loadResponse.value.contacts) {
                                                    if (dto.deletedActual || dto.fromGroupActual) {
                                                        deleteContactById(
                                                            ContactId(dto.id),
                                                            queries
                                                        )
                                                    } else {
                                                        upsertContact(dto, queries)
                                                        contactMap[ContactId(dto.id)] = dto
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                processChatsResponse = processChatDtos(
                                    loadResponse.value.chats,
                                    contactMap,
                                )

                                inviteLock.withLock {
                                    contactLock.withLock {
                                        queries.transaction {
                                            for (dto in loadResponse.value.invites) {
                                                updatedContactIds.add(ContactId(dto.contact_id))
                                                upsertInvite(dto, queries)
                                            }
                                        }
                                    }
                                }

                                subscriptionLock.withLock {
                                    queries.transaction {
                                        for (dto in loadResponse.value.subscriptions) {
                                            upsertSubscription(dto, queries)
                                        }
                                    }
                                }

                            }.join()

                            error?.let {
                                throw it
                            } ?: run {
                                if (
                                    loadResponse.value.contacts.size > 1 ||
                                    loadResponse.value.chats.isNotEmpty()
                                ) {
                                    authenticationStorage.putString(
                                        REPOSITORY_LAST_SEEN_CONTACTS_DATE,
                                        now
                                    )
                                }
                            }

                            emit(
                                if (processChatsResponse is Response.Success) {
                                    Response.Success(
                                        RestoreProgress(restoring, 4)
                                    )
                                } else {
                                    Response.Error(ResponseError("Failed to refresh contacts and chats"))
                                }
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

        var owner: Contact? = accountOwner.value

        if (owner == null) {
            try {
                accountOwner.collect {
                    if (it != null) {
                        owner = it
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
            }
            delay(25L)
        }

        if (owner?.id == null || owner!!.id == contactId) {
            val msg = "Account Owner was null, or deleteContactById was called for account owner."
            LOG.w(TAG, msg)
            return Response.Error(ResponseError(msg))
        }

        var deleteContactResponse: Response<Any, ResponseError> = Response.Success(Any())

        applicationScope.launch(mainImmediate) {
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
        contactKey: ContactKey?,
        photoUrl: PhotoUrl?
    ): Flow<LoadResponse<Any, ResponseError>> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        val postContactDto = PostContactDto(
            alias = contactAlias.value,
            public_key = lightningNodePubKey.value,
            status = ContactStatus.CONFIRMED.absoluteValue,
            route_hint = lightningRouteHint?.value,
            contact_key = contactKey?.value,
            photo_url = photoUrl?.value
        )

        val sharedFlow: MutableSharedFlow<Response<Boolean, ResponseError>> =
            MutableSharedFlow(1, 0)

        applicationScope.launch(mainImmediate) {

            networkQueryContact.createContact(postContactDto).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading -> {
                    }
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

    override suspend fun connectToContact(
        contactAlias: ContactAlias,
        lightningNodePubKey: LightningNodePubKey,
        lightningRouteHint: LightningRouteHint?,
        contactKey: ContactKey,
        message: String,
        photoUrl: PhotoUrl?,
        priceToMeet: Sat,
    ): Response<ContactId?, ResponseError> {
        var response: Response<ContactId?, ResponseError> = Response.Error(
            ResponseError("Something went wrong, please try again later")
        )

        applicationScope.launch(mainImmediate) {
            createContact(
                contactAlias,
                lightningNodePubKey,
                lightningRouteHint,
                contactKey,
                photoUrl
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }

                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        val contact = getContactByPubKey(lightningNodePubKey).firstOrNull()

                        response = if (contact != null) {
                            val messageBuilder = SendMessage.Builder()
                            messageBuilder.setText(message)
                            messageBuilder.setContactId(contact.id)
                            messageBuilder.setPriceToMeet(priceToMeet)

                            sendMessage(
                                messageBuilder.build().first
                            )

                            Response.Success(contact.id)
                        } else {
                            Response.Error(
                                ResponseError("Contact not found")
                            )
                        }
                    }
                }
            }
        }.join()

        return response
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
                            is LoadResponse.Loading -> {
                            }
                            is Response.Error -> {
                                response = loadResponse
                            }
                            is Response.Success -> {
                                contactLock.withLock {
                                    queries.transaction {
                                        upsertContact(loadResponse.value, queries)
                                    }
                                }
                                LOG.d(TAG, "Owner has been successfully updated")
                            }
                        }
                    }

                    throw Exception()
                }

            }
        } catch (e: Exception) {
        }

        return response
    }

    override suspend fun updateContact(
        contactId: ContactId,
        alias: ContactAlias?,
        routeHint: LightningRouteHint?
    ): Response<Any, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()
        var response: Response<Any, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            try {
                networkQueryContact.updateContact(
                    contactId,
                    PutContactDto(
                        alias = alias?.value,
                        route_hint = routeHint?.value
                    )
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }
                        is Response.Error -> {
                            response = loadResponse
                        }
                        is Response.Success -> {
                            contactLock.withLock {
                                queries.transaction {
                                    updatedContactIds.add(ContactId(loadResponse.value.id))
                                    upsertContact(loadResponse.value, queries)
                                }
                            }
                            response = loadResponse

                            LOG.d(TAG, "Contact has been successfully updated")
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.e(TAG, "Failed to update contact", e)

                response = Response.Error(ResponseError(e.message.toString()))
            }
        }.join()

        return response ?: Response.Error(ResponseError("Failed to update contact"))
    }

    override suspend fun forceKeyExchange(
        contactId: ContactId,
    ) {
        applicationScope.launch(mainImmediate) {
            try {
                networkQueryContact.exchangeKeys(
                    contactId,
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {}
                        is Response.Success -> {}
                    }
                }
            } catch (e: Exception) {
                LOG.e(TAG, "Failed to update contact", e)
            }
        }
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
                                is LoadResponse.Loading -> {
                                }
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
        } catch (e: Exception) {
        }

        return response
    }

    @OptIn(RawPasswordAccess::class)
    override suspend fun updateOwnerNameAndKey(
        name: String,
        contactKey: Password
    ): Response<Any, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()
        var response: Response<Any, ResponseError> = Response.Success(Any())

        val publicKey = StringBuilder().let { sb ->
            sb.append(contactKey.value)
            sb.toString()
        }

        try {
            accountOwner.collect { owner ->
                if (owner != null) {
                    networkQueryContact.updateContact(
                        owner.id,
                        PutContactDto(
                            alias = name,
                            contact_key = publicKey
                        )
                    ).collect { loadResponse ->
                        @Exhaustive
                        when (loadResponse) {
                            is LoadResponse.Loading -> {
                            }
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
                                LOG.d(TAG, "Owner name and key has been successfully updated")

                                throw Exception()
                            }
                        }
                    }

                }

            }
        } catch (e: Exception) {
        }

        return response
    }

    override suspend fun updateProfilePic(
        stream: InputStreamProvider,
        mediaType: MediaType,
        fileName: String,
        contentLength: Long?
    ): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError> = Response.Success(true)
        val memeServerHost = MediaHost.DEFAULT

        applicationScope.launch(mainImmediate) {
            try {
                val token = memeServerTokenHandler.retrieveAuthenticationToken(memeServerHost)
                    ?: throw RuntimeException("MemeServerAuthenticationToken retrieval failure")

                val networkResponse = networkQueryMemeServer.uploadAttachment(
                    authenticationToken = token,
                    mediaType = mediaType,
                    stream = stream,
                    fileName = fileName,
                    contentLength = contentLength,
                    memeServerHost = memeServerHost,
                )

                @Exhaustive
                when (networkResponse) {
                    is Response.Error -> {
                        response = networkResponse
                    }
                    is Response.Success -> {
                        val newUrl =
                            PhotoUrl("https://${memeServerHost.value}/public/${networkResponse.value.muid}")

                        // TODO: if chatId method argument is null, update owner record

                        var owner = accountOwner.value

                        if (owner == null) {
                            try {
                                accountOwner.collect { contact ->
                                    if (contact != null) {
                                        owner = contact
                                        throw Exception()
                                    }
                                }
                            } catch (e: Exception) {
                            }
                            delay(25L)
                        }

                        owner?.let { nnOwner ->

                            networkQueryContact.updateContact(
                                nnOwner.id,
                                PutContactDto(photo_url = newUrl.value)
                            ).collect { loadResponse ->

                                @Exhaustive
                                when (loadResponse) {
                                    is LoadResponse.Loading -> {
                                    }
                                    is Response.Error -> {
                                        response = loadResponse
                                    }
                                    is Response.Success -> {
                                        val queries = coreDB.getSphinxDatabaseQueries()

                                        contactLock.withLock {
                                            withContext(io) {
                                                queries.contactUpdatePhotoUrl(
                                                    newUrl,
                                                    nnOwner.id,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } ?: throw IllegalStateException("Failed to retrieve account owner")
                    }
                }
            } catch (e: Exception) {
                response = Response.Error(
                    ResponseError("Failed to update Profile Picture", e)
                )
            }
        }.join()

        return response
    }

    override suspend fun toggleContactBlocked(contact: Contact): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError> = Response.Success(!contact.isBlocked())

        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()
            val currentBlockedValue = contact.blocked

            contactLock.withLock {
                withContext(io) {
                    queries.contactUpdateBlocked(
                        if (currentBlockedValue.isTrue()) Blocked.False else Blocked.True,
                        contact.id
                    )
                }
            }

            networkQueryContact.toggleBlockedContact(
                contact.id,
                contact.blocked
            ).collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {}

                    is Response.Error -> {
                        response = loadResponse

                        contactLock.withLock {
                            withContext(io) {
                                queries.contactUpdateBlocked(
                                    currentBlockedValue,
                                    contact.id
                                )
                            }
                        }
                    }

                    is Response.Success -> {}
                }
            }
        }.join()

        return response
    }

    override suspend fun setGithubPat(
        pat: String
    ): Response<Boolean, ResponseError> {

        var response: Response<Boolean, ResponseError> = Response.Error(
            ResponseError("generate Github PAT failed to execute")
        )

        relayDataHandler.retrieveRelayTransportKey()?.let { key ->

            applicationScope.launch(mainImmediate) {

                val encryptionResponse = rsa.encrypt(
                    key,
                    UnencryptedString(pat),
                    formatOutput = false,
                    dispatcher = default,
                )

                @Exhaustive
                when (encryptionResponse) {
                    is Response.Error -> {}

                    is Response.Success -> {
                        networkQueryContact.generateGithubPAT(
                            GithubPATDto(
                                encryptionResponse.value.value
                            )
                        ).collect { loadResponse ->
                            @Exhaustive
                            when (loadResponse) {
                                is LoadResponse.Loading -> {}

                                is Response.Error -> {
                                    response = loadResponse
                                }
                                is Response.Success -> {
                                    response = Response.Success(true)
                                }
                            }
                        }
                    }
                }
            }.join()
        }

        return response
    }

    override suspend fun updateChatProfileInfo(
        chatId: ChatId,
        alias: ChatAlias?,
        profilePic: PublicAttachmentInfo?
    ): Response<ChatDto, ResponseError> {
        var response: Response<ChatDto, ResponseError> = Response.Error(
            ResponseError("updateChatProfileInfo failed to execute")
        )

        if (alias != null) {
            response = updateChatProfileAlias(chatId, alias)
        } else if (profilePic != null) {
            response = updateChatProfilePic(
                chatId,
                profilePic.stream,
                profilePic.mediaType,
                profilePic.fileName,
                profilePic.contentLength
            )
        }

        return response
    }

    suspend fun updateChatProfilePic(
        chatId: ChatId,
        stream: InputStreamProvider,
        mediaType: MediaType,
        fileName: String,
        contentLength: Long?
    ): Response<ChatDto, ResponseError> {
        var response: Response<ChatDto, ResponseError> = Response.Error(
            ResponseError("updateChatProfilePic failed to execute")
        )
        val memeServerHost = MediaHost.DEFAULT

        applicationScope.launch(mainImmediate) {
            try {
                val token = memeServerTokenHandler.retrieveAuthenticationToken(memeServerHost)
                    ?: throw RuntimeException("MemeServerAuthenticationToken retrieval failure")

                val networkResponse = networkQueryMemeServer.uploadAttachment(
                    authenticationToken = token,
                    mediaType = mediaType,
                    stream = stream,
                    fileName = fileName,
                    contentLength = contentLength,
                    memeServerHost = memeServerHost,
                )

                @Exhaustive
                when (networkResponse) {
                    is Response.Error -> {
                        response = networkResponse
                    }
                    is Response.Success -> {
                        val newUrl = PhotoUrl(
                            "https://${memeServerHost.value}/public/${networkResponse.value.muid}"
                        )

                        networkQueryChat.updateChat(
                            chatId,
                            PutChatDto(
                                my_photo_url = newUrl.value,
                            )
                        ).collect { loadResponse ->

                            @Exhaustive
                            when (loadResponse) {
                                is LoadResponse.Loading -> {
                                }
                                is Response.Error -> {
                                    response = loadResponse
                                }
                                is Response.Success -> {
                                    response = loadResponse
                                    val queries = coreDB.getSphinxDatabaseQueries()

                                    chatLock.withLock {
                                        withContext(io) {
                                            queries.transaction {
                                                upsertChat(
                                                    loadResponse.value,
                                                    moshi,
                                                    chatSeenMap,
                                                    queries,
                                                    null,
                                                    accountOwner.value?.nodePubKey
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                response = Response.Error(
                    ResponseError("Failed to update Chat Profile", e)
                )
            }
        }.join()

        LOG.d(TAG, "Completed Upload Returning: $response")
        return response
    }

    private suspend fun updateChatProfileAlias(
        chatId: ChatId,
        alias: ChatAlias?
    ): Response<ChatDto, ResponseError> {
        var response: Response<ChatDto, ResponseError> = Response.Error(
            ResponseError("updateChatProfilePic failed to execute")
        )

        applicationScope.launch(mainImmediate) {
            networkQueryChat.updateChat(
                chatId,
                PutChatDto(
                    my_alias = alias?.value
                )
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        chatLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertChat(
                                        loadResponse.value,
                                        moshi,
                                        chatSeenMap,
                                        queries,
                                        null,
                                        accountOwner.value?.nodePubKey
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        LOG.d(TAG, "Completed Upload Returning: $response")
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

    override suspend fun getAccountBalanceAll(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<NodeBalanceAll, ResponseError>> = flow {

        networkQueryLightning.getBalanceAll(
            relayData
        ).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                    emit(loadResponse)
                }
                is Response.Error -> {
                    emit(loadResponse)
                }
                is Response.Success -> {
                    val nodeBalanceAll = NodeBalanceAll(
                        Sat(loadResponse.value.local_balance),
                        Sat(loadResponse.value.remote_balance)
                    )
                    emit(Response.Success(nodeBalanceAll))
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
        return decryptString(messageContent.value)
    }

    @OptIn(RawPasswordAccess::class)
    private suspend fun decryptMediaKey(
        mediaKey: MediaKey
    ): Response<UnencryptedByteArray, ResponseError> {
        return decryptString(mediaKey.value)
    }

    @OptIn(RawPasswordAccess::class)
    private suspend fun decryptString(
        value: String
    ): Response<UnencryptedByteArray, ResponseError> {
        val privateKey: CharArray = authenticationCoreManager.getEncryptionKey()
            ?.privateKey
            ?.value
            ?: return Response.Error(
                ResponseError("EncryptionKey retrieval failed")
            )

        return rsa.decrypt(
            rsaPrivateKey = RsaPrivateKey(privateKey),
            text = EncryptedString(value),
            dispatcher = default
        )
    }

    @OptIn(UnencryptedDataAccess::class)
    private suspend fun mapMessageDboAndDecryptContentIfNeeded(
        queries: SphinxDatabaseQueries,
        messageDbo: MessageDbo,
        reactions: List<Message>? = null,
        purchaseItems: List<Message>? = null,
        replyMessage: ReplyUUID? = null,
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

                        val message: MessageDboWrapper =
                            messageDboPresenterMapper.mapFrom(messageDbo)

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
                            val response = decryptMediaKey(MediaKey(key.value))

                            @Exhaustive
                            when (response) {
                                is Response.Error -> {
                                    MessageMediaDboWrapper(mediaDbo).also {
                                        it._mediaKeyDecrypted = null
                                        it._mediaKeyDecryptionError = true
                                        it._mediaKeyDecryptionException = response.exception
                                        message._messageMedia = it
                                    }
                                }
                                is Response.Success -> {

                                    response.value
                                        .toUnencryptedString(trim = false)
                                        .value
                                        .toMediaKeyDecrypted()
                                        .let { decryptedKey ->

                                            message._messageMedia = MessageMediaDboWrapper(mediaDbo)
                                                .also {
                                                    it._mediaKeyDecrypted = decryptedKey

                                                    if (decryptedKey == null) {
                                                        it._mediaKeyDecryptionError = true
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
                            message._messageMedia = MessageMediaDboWrapper(mediaDbo)
                        }

                    }

                } ?: message.also {
                    it._messageMedia = MessageMediaDboWrapper(mediaDbo)
                }

            } // else do nothing
        }

        message._reactions = reactions
        message._purchaseItems = purchaseItems

        replyMessage?.value?.toMessageUUID()?.let { uuid ->
            queries.messageGetToShowByUUID(uuid).executeAsOneOrNull()?.let { replyDbo ->
                message._replyMessage = mapMessageDboAndDecryptContentIfNeeded(queries, replyDbo)
            }
        }

        return message
    }

    override fun getAllMessagesToShowByChatId(chatId: ChatId, limit: Long): Flow<List<Message>> =
        flow {
            val queries = coreDB.getSphinxDatabaseQueries()

            emitAll(
                (
                        if (limit > 0) {
                            queries.messageGetAllToShowByChatIdWithLimit(chatId, limit)
                        } else {
                            queries.messageGetAllToShowByChatId(chatId)
                        }
                        )
                    .asFlow()
                    .mapToList(io)
                    .map { listMessageDbo ->
                        withContext(default) {

                            val reactionsMap: MutableMap<MessageUUID, ArrayList<Message>> =
                                LinkedHashMap(listMessageDbo.size)

                            val purchaseItemsMap: MutableMap<MessageMUID, ArrayList<Message>> =
                                LinkedHashMap(listMessageDbo.size)

                            for (dbo in listMessageDbo) {
                                dbo.uuid?.let { uuid ->
                                    reactionsMap[uuid] = ArrayList(0)
                                }
                                dbo.muid?.let { muid ->
                                    purchaseItemsMap[muid] = ArrayList(0)
                                }
                            }

                            val replyUUIDs = reactionsMap.keys.map { ReplyUUID(it.value) }

                            val purchaseItemsMUIDs =
                                purchaseItemsMap.keys.map { MessageMUID(it.value) }

                            replyUUIDs.chunked(500).forEach { chunkedIds ->
                                queries.messageGetAllReactionsByUUID(
                                    chatId,
                                    chunkedIds,
                                ).executeAsList()
                                    .let { response ->
                                        response.forEach { dbo ->
                                            dbo.reply_uuid?.let { uuid ->
                                                reactionsMap[MessageUUID(uuid.value)]?.add(
                                                    mapMessageDboAndDecryptContentIfNeeded(
                                                        queries,
                                                        dbo
                                                    )
                                                )
                                            }
                                        }
                                    }
                            }

                            purchaseItemsMUIDs.chunked(500).forEach { chunkedMUIDs ->
                                queries.messageGetAllPurchaseItemsByMUID(
                                    chatId,
                                    chunkedMUIDs,
                                ).executeAsList()
                                    .let { response ->
                                        response.forEach { dbo ->
                                            dbo.muid?.let { muid ->
                                                purchaseItemsMap[muid]?.add(
                                                    mapMessageDboAndDecryptContentIfNeeded(
                                                        queries,
                                                        dbo
                                                    )
                                                )
                                            }
                                            dbo.original_muid?.let { original_muid ->
                                                purchaseItemsMap[original_muid]?.add(
                                                    mapMessageDboAndDecryptContentIfNeeded(
                                                        queries,
                                                        dbo
                                                    )
                                                )
                                            }
                                        }
                                    }
                            }

                            listMessageDbo.reversed().map { dbo ->
                                mapMessageDboAndDecryptContentIfNeeded(
                                    queries,
                                    dbo,
                                    dbo.uuid?.let { reactionsMap[it] },
                                    dbo.muid?.let { purchaseItemsMap[it] },
                                    dbo.reply_uuid,
                                )
                            }

                        }
                    }
            )
        }

    override fun getMessageById(messageId: MessageId): Flow<Message?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()
        emitAll(getMessageByIdImpl(messageId, queries))
    }

    private fun getMessageByIdImpl(
        messageId: MessageId,
        queries: SphinxDatabaseQueries
    ): Flow<Message?> =
        queries.messageGetById(messageId)
            .asFlow()
            .mapToOneOrNull(io)
            .map {
                it?.let { messageDbo ->
                    mapMessageDboAndDecryptContentIfNeeded(queries, messageDbo)
                }
            }
            .distinctUntilChanged()

    override fun searchMessagesBy(chatId: ChatId, term: String): Flow<List<Message>> = flow {
        emitAll(
            coreDB.getSphinxDatabaseQueries()
                .messagesSearchByTerm(
                    chatId,
                    "%${term.lowercase()}%"
                )
                .asFlow()
                .mapToList(io)
                .map { listMessageDbo ->
                    listMessageDbo.map {
                        messageDboPresenterMapper.mapFrom(it)
                    }
                }
                .distinctUntilChanged()
        )
    }

    override fun getTribeLastMemberRequestByContactId(
        contactId: ContactId,
        chatId: ChatId,
    ): Flow<Message?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        emitAll(
            queries.messageLastMemberRequestGetByContactId(contactId, chatId)
                .asFlow()
                .mapToOneOrNull(io)
                .map {
                    it?.let { messageDbo ->
                        mapMessageDboAndDecryptContentIfNeeded(queries, messageDbo)
                    }
                }
                .distinctUntilChanged()
        )
    }

    override fun getMessageByUUID(messageUUID: MessageUUID): Flow<Message?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()
        emitAll(
            queries.messageGetByUUID(messageUUID)
                .asFlow()
                .mapToOneOrNull(io)
                .map {
                    it?.let { messageDbo ->
                        mapMessageDboAndDecryptContentIfNeeded(queries, messageDbo)
                    }
                }
                .distinctUntilChanged()
        )
    }

    override suspend fun getAllMessagesByUUID(messageUUIDs: List<MessageUUID>): List<Message> {
        return coreDB.getSphinxDatabaseQueries()
            .messageGetAllByUUID(messageUUIDs)
            .executeAsList()
            .map { messageDboPresenterMapper.mapFrom(it) }
    }

    override fun updateMessageContentDecrypted(
        messageId: MessageId,
        messageContentDecrypted: MessageContentDecrypted
    ) {
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()
            messageLock.withLock {
                withContext(io) {
                    queries.transaction {
                        queries.messageUpdateContentDecrypted(
                            messageContentDecrypted,
                            messageId
                        )
                    }
                }
            }
        }
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

    private fun messageText(sendMessage: SendMessage, moshi: Moshi): String? {
        try {
            if (sendMessage.giphyData != null) {
                return sendMessage.giphyData?.let {
                    "${GiphyData.MESSAGE_PREFIX}${it.toJson(moshi).toByteArray().encodeBase64()}"
                }
            }
        } catch (e: Exception) {
            LOG.e(TAG, "GiphyData toJson failed: ", e)
        }

        try {
            if (sendMessage.podcastClip != null) {
                return sendMessage.podcastClip?.let {
                    "${PodcastClip.MESSAGE_PREFIX}${it.toJson(moshi)}"
                }
            }
        } catch (e: Exception) {
            LOG.e(TAG, "PodcastClip toJson failed: ", e)
        }

        return sendMessage.text
    }

    // TODO: Rework to handle different message types
    @OptIn(RawPasswordAccess::class)
    override fun sendMessage(sendMessage: SendMessage?) {
        if (sendMessage == null) return

        applicationScope.launch(mainImmediate) {

            val queries = coreDB.getSphinxDatabaseQueries()

            // TODO: Update SendMessage to accept a Chat && Contact instead of just IDs
            val chat: Chat? = sendMessage.chatId?.let {
                getChatById(it).firstOrNull()
            }

            val contact: Contact? = sendMessage.contactId?.let {
                getContactById(it).firstOrNull()
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
                    } catch (e: Exception) {
                    }
                    delay(25L)
                    owner
                }

            val ownerPubKey = owner?.rsaPublicKey

            if (owner == null) {
                LOG.w(TAG, "Owner returned null")
                return@launch
            }

            if (ownerPubKey == null) {
                LOG.w(TAG, "Owner's RSA public key was null")
                return@launch
            }

            // encrypt text
            val message: Pair<MessageContentDecrypted, MessageContent>? =
                messageText(sendMessage, moshi)?.let { msgText ->

                    val response = rsa.encrypt(
                        ownerPubKey,
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
                            Pair(
                                MessageContentDecrypted(msgText),
                                MessageContent(response.value.value)
                            )
                        }
                    }
                }

            // media attachment
            val media: Triple<Password, MediaKey, AttachmentInfo>? =
                if (sendMessage.giphyData == null) {
                    sendMessage.attachmentInfo?.let { info ->
                        val password = PasswordGenerator(MEDIA_KEY_SIZE).password

                        val response = rsa.encrypt(
                            ownerPubKey,
                            UnencryptedString(password.value.joinToString("")),
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
                                Triple(password, MediaKey(response.value.value), info)
                            }
                        }
                    }
                } else {
                    null
                }

            if (message == null && media == null && !sendMessage.isTribePayment) {
                return@launch
            }

            val pricePerMessage = chat?.pricePerMessage?.value ?: 0
            val escrowAmount = chat?.escrowAmount?.value ?: 0
            val priceToMeet = sendMessage.priceToMeet?.value ?: 0
            val messagePrice = (pricePerMessage + escrowAmount + priceToMeet).toSat() ?: Sat(0)

            val messageType = when {
                (media != null) -> {
                    MessageType.Attachment
                }
                (sendMessage.isBoost) -> {
                    MessageType.Boost
                }
                (sendMessage.isTribePayment) -> {
                    MessageType.DirectPayment
                }
                else -> {
                    MessageType.Message
                }
            }

            //If is tribe payment, reply UUID is sent to identify recipient. But it's not a response
            val replyUUID = when {
                (sendMessage.isTribePayment) -> {
                    null
                }
                else -> {
                    sendMessage.replyUUID
                }
            }

            val provisionalMessageId: MessageId? = chat?.let { chatDbo ->
                // Build provisional message and insert
                provisionalMessageLock.withLock {
                    val currentProvisionalId: MessageId? = withContext(io) {
                        queries.messageGetLowestProvisionalMessageId().executeAsOneOrNull()
                    }

                    val provisionalId = MessageId((currentProvisionalId?.value ?: 0L) - 1)

                    withContext(io) {

                        queries.transaction {

                            if (media != null) {
                                queries.messageMediaUpsert(
                                    media.second,
                                    media.third.mediaType,
                                    MediaToken.PROVISIONAL_TOKEN,
                                    provisionalId,
                                    chatDbo.id,
                                    MediaKeyDecrypted(media.first.value.joinToString("")),
                                    media.third.file,
                                    sendMessage.attachmentInfo?.fileName
                                )
                            }

                            queries.messageUpsert(
                                MessageStatus.Pending,
                                Seen.True,
                                chatDbo.myAlias?.value?.toSenderAlias(),
                                chatDbo.myPhotoUrl,
                                null,
                                replyUUID,
                                messageType,
                                null,
                                null,
                                Push.False,
                                null,
                                provisionalId,
                                null,
                                chatDbo.id,
                                owner.id,
                                sendMessage.contactId,
                                sendMessage.tribePaymentAmount ?: messagePrice,
                                null,
                                null,
                                DateTime.nowUTC().toDateTime(),
                                null,
                                message?.second,
                                message?.first,
                                null,
                                false.toFlagged()
                            )

                            if (media != null) {
                                queries.messageMediaUpsert(
                                    media.second,
                                    media.third.mediaType,
                                    MediaToken.PROVISIONAL_TOKEN,
                                    provisionalId,
                                    chatDbo.id,
                                    MediaKeyDecrypted(media.first.value.joinToString("")),
                                    media.third.file,
                                    sendMessage.attachmentInfo?.fileName
                                )
                            }
                        }

                        provisionalId
                    }
                }
            }

            val isPaidTextMessage =
                sendMessage.attachmentInfo?.mediaType?.isSphinxText == true &&
                        sendMessage.paidMessagePrice?.value ?: 0 > 0

            val messageContent: String? = if (isPaidTextMessage) null else message?.second?.value

            val remoteTextMap: Map<String, String>? =
                if (isPaidTextMessage) null else getRemoteTextMap(
                    UnencryptedString(message?.first?.value ?: ""),
                    contact,
                    chat
                )

            val mediaKeyMap: Map<String, String>? = if (media != null) {
                getMediaKeyMap(
                    owner.id,
                    media.second,
                    UnencryptedString(media.first.value.joinToString("")),
                    contact,
                    chat
                )
            } else {
                null
            }

            val postMemeServerDto: PostMemeServerUploadDto? = if (media != null) {
                val token = memeServerTokenHandler.retrieveAuthenticationToken(MediaHost.DEFAULT)
                    ?: provisionalMessageId?.let { provId ->
                        withContext(io) {
                            queries.messageUpdateStatus(MessageStatus.Failed, provId)
                        }

                        return@launch
                    } ?: return@launch

                val response = networkQueryMemeServer.uploadAttachmentEncrypted(
                    token,
                    media.third.mediaType,
                    media.third.file,
                    media.third.fileName,
                    media.first,
                    MediaHost.DEFAULT,
                )

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        LOG.e(TAG, response.message, response.exception)

                        provisionalMessageId?.let { provId ->
                            withContext(io) {
                                queries.messageUpdateStatus(MessageStatus.Failed, provId)
                            }
                        }

                        return@launch
                    }
                    is Response.Success -> {
                        response.value
                    }
                }
            } else {
                null
            }

            val amount = messagePrice.value + (sendMessage.tribePaymentAmount ?: Sat(0)).value

            val postMessageDto: PostMessageDto = try {
                PostMessageDto(
                    sendMessage.chatId?.value,
                    sendMessage.contactId?.value,
                    amount,
                    messagePrice.value,
                    sendMessage.replyUUID?.value,
                    messageContent,
                    remoteTextMap,
                    mediaKeyMap,
                    postMemeServerDto?.mime,
                    postMemeServerDto?.muid,
                    sendMessage.paidMessagePrice?.value,
                    sendMessage.isBoost,
                    sendMessage.isTribePayment
                )
            } catch (e: IllegalArgumentException) {
                LOG.e(TAG, "Failed to create PostMessageDto", e)

                provisionalMessageId?.let { provId ->
                    withContext(io) {
                        queries.messageUpdateStatus(MessageStatus.Failed, provId)
                    }
                }

                return@launch
            }

            sendMessage(
                provisionalMessageId,
                postMessageDto,
                message?.first,
                media
            )
        }
    }

    private suspend fun getRemoteTextMap(
        unencryptedString: UnencryptedString?,
        contact: Contact?,
        chat: Chat?,
    ): Map<String, String>? {

        return if (unencryptedString != null) {
            contact?.id?.let { nnContactId ->
                // we know it's a conversation as the contactId is always sent
                contact.rsaPublicKey?.let { pubKey ->

                    val response = rsa.encrypt(
                        pubKey,
                        unencryptedString,
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

            } ?: chat?.groupKey?.value?.let { rsaPubKeyString ->
                val response = rsa.encrypt(
                    RsaPublicKey(rsaPubKeyString.toCharArray()),
                    unencryptedString,
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
        } else {
            null
        }
    }

    private suspend fun getMediaKeyMap(
        ownerId: ContactId,
        mediaKey: MediaKey,
        unencryptedMediaKey: UnencryptedString?,
        contact: Contact?,
        chat: Chat?,
    ): Map<String, String>? {

        return if (unencryptedMediaKey != null) {
            val map: MutableMap<String, String> = LinkedHashMap(2)

            map[ownerId.value.toString()] = mediaKey.value

            contact?.id?.let { nnContactId ->
                // we know it's a conversation as the contactId is always sent
                contact.rsaPublicKey?.let { pubKey ->

                    val response = rsa.encrypt(
                        pubKey,
                        unencryptedMediaKey,
                        formatOutput = false,
                        dispatcher = default,
                    )

                    @Exhaustive
                    when (response) {
                        is Response.Error -> {
                            LOG.e(TAG, response.message, response.exception)
                        }
                        is Response.Success -> {
                            map[nnContactId.value.toString()] = response.value.value
                        }
                    }
                }

            } ?: chat?.groupKey?.value?.let { rsaPubKeyString ->
                val response = rsa.encrypt(
                    RsaPublicKey(rsaPubKeyString.toCharArray()),
                    unencryptedMediaKey,
                    formatOutput = false,
                    dispatcher = default,
                )

                @Exhaustive
                when (response) {
                    is Response.Error -> {
                        LOG.e(TAG, response.message, response.exception)
                    }
                    is Response.Success -> {
                        map["chat"] = response.value.value
                    }
                }
            }

            map
        } else {
            null
        }
    }

    @OptIn(RawPasswordAccess::class)
    suspend fun sendMessage(
        provisionalMessageId: MessageId?,
        postMessageDto: PostMessageDto,
        messageContentDecrypted: MessageContentDecrypted?,
        media: Triple<Password, MediaKey, AttachmentInfo>?,
    ) {
        val queries = coreDB.getSphinxDatabaseQueries()

        networkQueryMessage.sendMessage(postMessageDto).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                }
                is Response.Error -> {
                    LOG.e(TAG, loadResponse.message, loadResponse.exception)

                    messageLock.withLock {
                        provisionalMessageId?.let { provId ->
                            withContext(io) {
                                queries.messageUpdateStatus(MessageStatus.Failed, provId)
                            }
                        }
                    }

                }
                is Response.Success -> {

                    loadResponse.value.apply {
                        if (media != null) {
                            setMediaKeyDecrypted(media.first.value.joinToString(""))
                            setMediaLocalFile(media.third.file)
                        }

                        if (messageContentDecrypted != null) {
                            setMessageContentDecrypted(messageContentDecrypted.value)
                        }
                    }

                    chatLock.withLock {
                        messageLock.withLock {
                            contactLock.withLock {
                                withContext(io) {
                                    queries.transaction {
                                        // chat is returned only if this is the
                                        // first message sent to a new contact
                                        loadResponse.value.chat?.let { chatDto ->
                                            upsertChat(
                                                chatDto,
                                                moshi,
                                                chatSeenMap,
                                                queries,
                                                loadResponse.value.contact,
                                                accountOwner.value?.nodePubKey
                                            )
                                        }

                                        loadResponse.value.contact?.let { contactDto ->
                                            upsertContact(contactDto, queries)
                                        }

                                        upsertMessage(
                                            loadResponse.value,
                                            queries,
                                            media?.third?.fileName
                                        )

                                        provisionalMessageId?.let { provId ->
                                            deleteMessageById(provId, queries)
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

    override fun resendMessage(message: Message, chat: Chat) {

        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()

            val pricePerMessage = chat.pricePerMessage?.value ?: 0
            val escrowAmount = chat.escrowAmount?.value ?: 0
            val messagePrice = (pricePerMessage + escrowAmount).toSat() ?: Sat(0)

            val contact: Contact? = if (chat.type.isConversation()) {
                chat.contactIds.elementAtOrNull(1)?.let { contactId ->
                    getContactById(contactId).firstOrNull()
                }
            } else {
                null
            }

            val remoteTextMap: Map<String, String>? = getRemoteTextMap(
                UnencryptedString(message.messageContentDecrypted?.value ?: ""),
                contact,
                chat
            )

            val postMessageDto: PostMessageDto = try {
                PostMessageDto(
                    message.chatId.value,
                    contact?.id?.value,
                    messagePrice.value,
                    messagePrice.value,
                    message.replyUUID?.value,
                    message.messageContentDecrypted?.value,
                    remoteTextMap,
                    null,
                    message.messageMedia?.mediaType?.value,
                    message.messageMedia?.muid?.value,
                    null,
                    false
                )
            } catch (e: IllegalArgumentException) {
                LOG.e(TAG, "Failed to create PostMessageDto", e)

                withContext(io) {
                    queries.messageUpdateStatus(MessageStatus.Failed, message.id)
                }

                return@launch
            }

            sendMessage(
                message.id,
                postMessageDto,
                message.messageContentDecrypted,
                null
            )
        }
    }

    override fun flagMessage(message: Message, chat: Chat) {
        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()

            messageLock.withLock {
                withContext(io) {
                    queries.messageUpdateFlagged(
                        true.toFlagged(),
                        message.id
                    )
                }
            }

            val supportContactPubKey = LightningNodePubKey(
                "023d70f2f76d283c6c4e58109ee3a2816eb9d8feb40b23d62469060a2b2867b77f"
            )

            getContactByPubKey(supportContactPubKey).firstOrNull()?.let { supportContact ->
                val messageSender = getContactById(message.sender).firstOrNull()

                var flagMessageContent =
                    "Message Flagged\n- Message: ${message.uuid?.value ?: "Empty Message UUID"}\n- Sender: ${messageSender?.nodePubKey?.value ?: "Empty Sender"}"

                if (chat.isTribe()) {
                    flagMessageContent += "\n- Tribe: ${chat.uuid.value}"
                }

                val messageBuilder = SendMessage.Builder()
                messageBuilder.setText(flagMessageContent.trimIndent())

                messageBuilder.setContactId(supportContact.id)

                getConversationByContactId(supportContact.id).firstOrNull()
                    ?.let { supportContactChat ->
                        messageBuilder.setChatId(supportContactChat.id)
                    }

                sendMessage(
                    messageBuilder.build().first
                )
            }
        }
    }

    override suspend fun deleteMessage(message: Message): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError> = Response.Success(true)

        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()

            if (message.id.isProvisionalMessage) {
                messageLock.withLock {
                    withContext(io) {
                        queries.transaction {
                            deleteMessageById(message.id, queries)
                        }
                    }
                }
            } else {
                networkQueryMessage.deleteMessage(message.id).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }
                        is Response.Error -> {
                            response = Response.Error(
                                ResponseError(loadResponse.message, loadResponse.exception)
                            )
                        }
                        is Response.Success -> {
                            messageLock.withLock {
                                withContext(io) {
                                    queries.transaction {
                                        upsertMessage(loadResponse.value, queries)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response
    }

    override suspend fun sendPayment(
        sendPayment: SendPayment?
    ): Response<Any, ResponseError> {

        var response: Response<Any, ResponseError> = Response.Success(true)

        if (sendPayment == null) {
            response = Response.Error(
                ResponseError("Payment params cannot be null")
            )
            return response
        }

        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()

            val contact: ContactDbo? = sendPayment.contactId?.let {
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
                    } catch (e: Exception) {
                    }
                    delay(25L)
                    owner
                }

            if (owner == null) {
                response = Response.Error(
                    ResponseError("Owner cannot be null")
                )
                return@launch
            }

            var encryptedText: MessageContent? = null
            var encryptedRemoteText: MessageContent? = null

            sendPayment.text?.let { msgText ->
                encryptedText = owner
                    .rsaPublicKey
                    ?.let { pubKey ->
                        val encResponse = rsa.encrypt(
                            pubKey,
                            UnencryptedString(msgText),
                            formatOutput = false,
                            dispatcher = default,
                        )

                        @Exhaustive
                        when (encResponse) {
                            is Response.Error -> {
                                LOG.e(TAG, encResponse.message, encResponse.exception)
                                null
                            }
                            is Response.Success -> {
                                MessageContent(encResponse.value.value)
                            }
                        }
                    }

                contact?.let { contact ->
                    encryptedRemoteText = contact
                        .public_key
                        ?.let { pubKey ->
                            val encResponse = rsa.encrypt(
                                pubKey,
                                UnencryptedString(msgText),
                                formatOutput = false,
                                dispatcher = default,
                            )

                            @Exhaustive
                            when (encResponse) {
                                is Response.Error -> {
                                    LOG.e(TAG, encResponse.message, encResponse.exception)
                                    null
                                }
                                is Response.Success -> {
                                    MessageContent(encResponse.value.value)
                                }
                            }
                        }
                }
            }

            val postPaymentDto: PostPaymentDto = try {
                PostPaymentDto(
                    chat_id = sendPayment.chatId?.value,
                    contact_id = sendPayment.contactId?.value,
                    amount = sendPayment.amount,
                    text = encryptedText?.value,
                    remote_text = encryptedRemoteText?.value,
                    destination_key = sendPayment.destinationKey?.value,
                    muid = sendPayment.paymentTemplate?.muid,
                    dimensions = sendPayment.paymentTemplate?.getDimensions(),
                    media_type = sendPayment.paymentTemplate?.getMediaType()
                )
            } catch (e: IllegalArgumentException) {
                response = Response.Error(
                    ResponseError("Failed to create PostPaymentDto")
                )
                return@launch
            }

            if (postPaymentDto.isKeySendPayment) {
                networkQueryMessage.sendKeySendPayment(
                    postPaymentDto,
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }
                        is Response.Error -> {
                            LOG.e(TAG, loadResponse.message, loadResponse.exception)
                            response = loadResponse
                        }
                        is Response.Success -> {
                            response = loadResponse
                        }
                    }
                }
            } else {
                networkQueryMessage.sendPayment(
                    postPaymentDto,
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }
                        is Response.Error -> {
                            LOG.e(TAG, loadResponse.message, loadResponse.exception)
                            response = loadResponse
                        }
                        is Response.Success -> {
                            val message = loadResponse.value

                            decryptMessageDtoContentIfAvailable(
                                message,
                                coroutineScope { this },
                            )

                            chatLock.withLock {
                                messageLock.withLock {
                                    withContext(io) {

                                        queries.transaction {
                                            upsertMessage(message, queries)

                                            if (message.updateChatDboLatestMessage) {
                                                message.chat_id?.toChatId()?.let { chatId ->
                                                    updateChatDboLatestMessage(
                                                        message,
                                                        chatId,
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
            }

        }.join()

        return response
    }

    override suspend fun sendTribePayment(
        chatId: ChatId,
        amount: Sat,
        messageUUID: MessageUUID,
        text: String,
    ) {
        applicationScope.launch(mainImmediate) {

            val sendMessageBuilder = SendMessage.Builder()
            sendMessageBuilder.setChatId(chatId)
            sendMessageBuilder.setTribePaymentAmount(amount)
            sendMessageBuilder.setText(text)
            sendMessageBuilder.setReplyUUID(messageUUID.value.toReplyUUID())
            sendMessageBuilder.setIsTribePayment(true)

            sendMessage(
                sendMessageBuilder.build().first
            )
        }
    }

    override suspend fun boostMessage(
        chatId: ChatId,
        pricePerMessage: Sat,
        escrowAmount: Sat,
        messageUUID: MessageUUID,
    ): Response<Any, ResponseError> {

        var response: Response<Any, ResponseError> = Response.Success(true)

        applicationScope.launch(mainImmediate) {
            val owner: Contact = accountOwner.value.let {
                if (it != null) {
                    it
                } else {
                    var owner: Contact? = null
                    val retrieveOwnerJob = applicationScope.launch(mainImmediate) {
                        try {
                            accountOwner.collect { contact ->
                                if (contact != null) {
                                    owner = contact
                                    throw Exception()
                                }
                            }
                        } catch (e: Exception) {
                        }
                        delay(20L)
                    }

                    delay(200L)
                    retrieveOwnerJob.cancelAndJoin()

                    owner ?: let {
                        response = Response.Error(
                            ResponseError("Owner Contact returned null")
                        )
                        return@launch
                    }
                }
            }

            networkQueryMessage.boostMessage(
                boostMessageDto = PostBoostMessageDto(
                    chat_id = chatId.value,
                    amount = pricePerMessage.value + escrowAmount.value + (owner.tipAmount ?: Sat(
                        20L
                    )).value,
                    message_price = pricePerMessage.value + escrowAmount.value,
                    reply_uuid = messageUUID.value
                )
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        LOG.e(TAG, loadResponse.message, loadResponse.exception)
                        response = loadResponse
                    }
                    is Response.Success -> {
                        decryptMessageDtoContentIfAvailable(
                            loadResponse.value,
                            coroutineScope { this },
                        )
                        val queries = coreDB.getSphinxDatabaseQueries()
                        chatLock.withLock {
                            messageLock.withLock {
                                withContext(io) {

                                    queries.transaction {
                                        upsertMessage(loadResponse.value, queries)

                                        if (loadResponse.value.updateChatDboLatestMessage) {
                                            updateChatDboLatestMessage(
                                                loadResponse.value,
                                                chatId,
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
        }.join()

        return response
    }

    override fun sendBoost(
        chatId: ChatId,
        boost: FeedBoost
    ) {
        applicationScope.launch(mainImmediate) {
            val message = boost.toJson(moshi)

            val sendMessageBuilder = SendMessage.Builder()
            sendMessageBuilder.setChatId(chatId)
            sendMessageBuilder.setText(message)
            sendMessageBuilder.setIsBoost(true)

            sendMessage(
                sendMessageBuilder.build().first
            )
        }
    }

    override suspend fun sendPaymentRequest(requestPayment: SendPaymentRequest): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()

            val contact: ContactDbo? = requestPayment.contactId?.let {
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
                    } catch (e: Exception) {
                    }
                    delay(25L)
                    owner
                }

            if (owner == null) {
                response = Response.Error(
                    ResponseError("Owner cannot be null")
                )
                return@launch
            }

            var encryptedMemo: MessageContent? = null
            var encryptedRemoteMemo: MessageContent? = null

            requestPayment.memo?.let { msgText ->
                encryptedMemo = owner
                    .rsaPublicKey
                    ?.let { pubKey ->
                        val encResponse = rsa.encrypt(
                            pubKey,
                            UnencryptedString(msgText),
                            formatOutput = false,
                            dispatcher = default,
                        )

                        @Exhaustive
                        when (encResponse) {
                            is Response.Error -> {
                                LOG.e(TAG, encResponse.message, encResponse.exception)
                                null
                            }
                            is Response.Success -> {
                                MessageContent(encResponse.value.value)
                            }
                        }
                    }

                contact?.let { contact ->
                    encryptedRemoteMemo = contact
                        .public_key
                        ?.let { pubKey ->
                            val encResponse = rsa.encrypt(
                                pubKey,
                                UnencryptedString(msgText),
                                formatOutput = false,
                                dispatcher = default,
                            )

                            @Exhaustive
                            when (encResponse) {
                                is Response.Error -> {
                                    LOG.e(TAG, encResponse.message, encResponse.exception)
                                    null
                                }
                                is Response.Success -> {
                                    MessageContent(encResponse.value.value)
                                }
                            }
                        }
                }
            }

            val postRequestPaymentDto = PostPaymentRequestDto(
                requestPayment.chatId?.value,
                requestPayment.contactId?.value,
                requestPayment.amount,
                encryptedMemo?.value,
                encryptedRemoteMemo?.value
            )

            networkQueryMessage.sendPaymentRequest(postRequestPaymentDto).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        LOG.e(TAG, loadResponse.message, loadResponse.exception)
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = Response.Success(true)

                        val message = loadResponse.value

                        decryptMessageDtoContentIfAvailable(
                            message,
                            coroutineScope { this },
                        )

                        chatLock.withLock {
                            messageLock.withLock {
                                withContext(io) {

                                    queries.transaction {
                                        upsertMessage(message, queries)

                                        if (message.updateChatDboLatestMessage) {
                                            message.chat_id?.toChatId()?.let { chatId ->
                                                updateChatDboLatestMessage(
                                                    message,
                                                    chatId,
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
        }.join()

        return response ?: Response.Error(ResponseError("Failed to send payment request"))
    }

    override suspend fun payPaymentRequest(message: Message): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError>? = null

        message.paymentRequest?.let { lightningPaymentRequest ->
            applicationScope.launch(mainImmediate) {
                val queries = coreDB.getSphinxDatabaseQueries()

                val putPaymentRequestDto = PutPaymentRequestDto(
                    lightningPaymentRequest.value,
                )

                networkQueryMessage.payPaymentRequest(
                    putPaymentRequestDto,
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = Response.Error(
                                ResponseError(loadResponse.message, loadResponse.exception)
                            )
                        }
                        is Response.Success -> {
                            response = loadResponse

                            val message = loadResponse.value

                            messageLock.withLock {
                                withContext(io) {
                                    queries.transaction {
                                        upsertMessage(message, queries)

                                        if (message.updateChatDboLatestMessage) {
                                            message.chat_id?.toChatId()?.let { chatId ->
                                                updateChatDboLatestMessage(
                                                    message,
                                                    chatId,
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
            }.join()
        }

        return response ?: Response.Error(ResponseError("Failed to pay invoice"))
    }

    override suspend fun payAttachment(message: Message): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()

            message.messageMedia?.mediaToken?.let { mediaToken ->
                mediaToken.getPriceFromMediaToken().let { price ->

                    networkQueryMessage.payAttachment(
                        message.chatId,
                        message.sender,
                        price,
                        mediaToken
                    ).collect { loadResponse ->
                        @Exhaustive
                        when (loadResponse) {
                            is LoadResponse.Loading -> {
                            }

                            is Response.Error -> {
                                response = Response.Error(
                                    ResponseError(loadResponse.message, loadResponse.exception)
                                )
                            }
                            is Response.Success -> {
                                response = loadResponse

                                messageLock.withLock {
                                    withContext(io) {
                                        queries.transaction {
                                            upsertMessage(loadResponse.value, queries)
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }.join()

        return response ?: Response.Error(ResponseError("Failed to pay for attachment"))
    }

    override suspend fun setNotificationLevel(
        chat: Chat,
        level: NotificationLevel
    ): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError> = Response.Success(level.isMuteChat())

        applicationScope.launch(mainImmediate) {
            val queries = coreDB.getSphinxDatabaseQueries()
            val currentNotificationLevel = chat.notify

            chatLock.withLock {
                withContext(io) {
                    queries.transaction {
                        updateChatNotificationLevel(
                            chat.id,
                            level,
                            queries
                        )
                    }
                }
            }

            networkQueryChat.setNotificationLevel(chat.id, level).collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Success -> {}
                    is Response.Error -> {
                        response = loadResponse

                        chatLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    updateChatNotificationLevel(
                                        chat.id,
                                        currentNotificationLevel,
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

    override suspend fun updateChatContentSeenAt(chatId: ChatId) {
        val queries = coreDB.getSphinxDatabaseQueries()

        chatLock.withLock {
            withContext(io) {
                queries.chatUpdateContentSeenAt(
                    DateTime(Date()),
                    chatId
                )
            }
        }
    }

    override fun joinTribe(
        tribeDto: TribeDto
    ): Flow<LoadResponse<ChatDto, ResponseError>> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()
        var response: Response<ChatDto, ResponseError>? = null
        val memeServerHost = MediaHost.DEFAULT

        emit(LoadResponse.Loading)

        applicationScope.launch(mainImmediate) {

            tribeDto.myPhotoUrl = tribeDto.profileImgFile?.let { imgFile ->
                // If an image file is provided we should upload it
                val token = memeServerTokenHandler.retrieveAuthenticationToken(memeServerHost)
                    ?: throw RuntimeException("MemeServerAuthenticationToken retrieval failure")

                val networkResponse = networkQueryMemeServer.uploadAttachment(
                    authenticationToken = token,
                    mediaType = MediaType.Image("${MediaType.IMAGE}/${imgFile.extension}"),
                    stream = object : InputStreamProvider() {
                        override fun newInputStream(): InputStream = imgFile.inputStream()
                    },
                    fileName = imgFile.name,
                    contentLength = imgFile.length(),
                    memeServerHost = memeServerHost,
                )
                @Exhaustive
                when (networkResponse) {
                    is Response.Error -> {
                        LOG.e(TAG, "Failed to upload image: ", networkResponse.exception)
                        response = networkResponse
                        null
                    }
                    is Response.Success -> {
                        "https://${memeServerHost.value}/public/${networkResponse.value.muid}"
                    }
                }
            }

            networkQueryChat.joinTribe(tribeDto).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        chatLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertChat(
                                        loadResponse.value,
                                        moshi,
                                        chatSeenMap,
                                        queries,
                                        null,
                                        accountOwner.value?.nodePubKey
                                    )
                                    updateChatTribeData(
                                        tribeDto,
                                        ChatId(loadResponse.value.id),
                                        queries
                                    )
                                }
                            }
                        }

                        response = loadResponse
                    }
                }
            }

        }.join()

        emit(response ?: Response.Error(ResponseError("")))
    }

    override suspend fun updateTribeInfo(chat: Chat): TribeData? {
        var owner: Contact? = accountOwner.value

        if (owner == null) {
            try {
                accountOwner.collect {
                    if (it != null) {
                        owner = it
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
            }
            delay(25L)
        }

        var tribeData: TribeData? = null

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

                            if (owner?.nodePubKey != chat.ownerPubKey) {
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
                                    networkQueryChat.updateTribe(
                                        chat.id,
                                        PostGroupDto(
                                            tribeDto.name,
                                            tribeDto.description,
                                            img = tribeDto.img ?: "",
                                        )
                                    ).collect {}
                                }

                            }

                            chat.host?.let { host ->
                                tribeDto.feed_url?.toFeedUrl()?.let { feedUrl ->
                                    val feedType = (tribeDto.feed_type ?: 0).toFeedType()

                                    tribeData = TribeData(host, chat.uuid, feedUrl, feedType)
                                }
                            }
                        }
                    }
                }
            }
        }

        return tribeData
    }

    private val podcastLock = Mutex()
    override suspend fun updateFeedContent(
        chatId: ChatId,
        host: ChatHost,
        feedUrl: FeedUrl,
        searchResultDescription: FeedDescription?,
        searchResultImageUrl: PhotoUrl?,
        chatUUID: ChatUUID?,
        subscribed: Subscribed,
        currentItemId: FeedId?
    ): Response<FeedId, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()

        var updateResponse: Response<FeedId, ResponseError> = Response.Error(ResponseError("Feed content update failed"))

        networkQueryChat.getFeedContent(
            host,
            feedUrl,
            chatUUID
        ).collect { response ->
            @Exhaustive
            when (response) {
                is LoadResponse.Loading -> {}

                is Response.Error -> {
                    updateResponse = response
                }
                is Response.Success -> {

                    var cId: ChatId = chatId
                    val feedId = response.value.id.toFeedId()

                    feedId?.let { feedId ->
                        queries.feedGetByIds(
                            feedId.youtubeFeedIds()
                        ).executeAsOneOrNull()
                            ?.let { existingFeed ->
                                //If feed already exists linked to a chat, do not override with NULL CHAT ID
                                if (chatId.value == ChatId.NULL_CHAT_ID.toLong()) {
                                    cId = existingFeed.chat_id
                                }
                            }
                    }

                    podcastLock.withLock {
                        queries.transaction {
                            upsertFeed(
                                response.value,
                                feedUrl,
                                searchResultDescription,
                                searchResultImageUrl,
                                cId,
                                currentItemId,
                                subscribed,
                                queries
                            )
                        }
                    }

                    delay(500L)

                    updateResponse = feedId?.let {
                        Response.Success(it)
                    } ?: run {
                       Response.Error(ResponseError("Feed content update failed"))
                    }
                }
            }
        }

        return updateResponse
    }

    override fun getFeedByChatId(chatId: ChatId): Flow<Feed?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        queries.feedGetByChatId(chatId)
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.let { feedDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
            .collect { value: Feed? ->
                value?.let { feed ->
                    emit(
                        mapFeedDbo(
                            feed,
                            queries
                        )
                    )
                }
            }
    }

    override fun getFeedById(feedId: FeedId): Flow<Feed?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        queries.feedGetByIds(feedId.youtubeFeedIds())
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.let { feedDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
            .collect { value: Feed? ->
                value?.let { feed ->
                    emit(
                        mapFeedDbo(
                            feed,
                            queries
                        )
                    )
                }
            }
    }

    override fun getFeedItemById(feedItemId: FeedId): Flow<FeedItem?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        queries.feedItemGetById(feedItemId)
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.let { feedItemDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
            .collect { value: FeedItem? ->
                value?.let { feedItem ->
                    emit(feedItem)
                }
            }
    }

    private val feedDboPresenterMapper: FeedDboPresenterMapper by lazy {
        FeedDboPresenterMapper(dispatchers)
    }
    private val feedItemDboPresenterMapper: FeedItemDboPresenterMapper by lazy {
        FeedItemDboPresenterMapper(dispatchers)
    }
    private val feedModelDboPresenterMapper: FeedModelDboPresenterMapper by lazy {
        FeedModelDboPresenterMapper(dispatchers)
    }
    private val feedDestinationDboPresenterMapper: FeedDestinationDboPresenterMapper by lazy {
        FeedDestinationDboPresenterMapper(dispatchers)
    }

    override fun getAllFeedsOfType(feedType: FeedType): Flow<List<Feed>> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        emitAll(
            queries.feedGetAllByFeedType(feedType)
                .asFlow()
                .mapToList(io)
                .map { listFeedDbo ->
                    withContext(default) {
                        mapFeedDboList(
                            listFeedDbo, queries
                        )
                    }
                }
        )
    }

    override fun getAllFeeds(): Flow<List<Feed>> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()
        emitAll(
            queries.feedGetAllSubscribed()
                .asFlow()
                .mapToList(io)
                .map { listFeedDbo ->
                    withContext(default) {
                        mapFeedDboList(
                            listFeedDbo, queries
                        )
                    }
                }
        )
    }

    override val recommendationsPodcast: MutableStateFlow<Podcast?> by lazy {
        MutableStateFlow(null)
    }

    override fun getRecommendedFeeds(): Flow<List<FeedRecommendation>> = flow {

        var results: MutableList<FeedRecommendation> = mutableListOf()

        applicationScope.launch(mainImmediate) {
            networkQueryFeedSearch.getFeedRecommendations().collect { response ->
                @Exhaustive
                when (response) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {}
                    is Response.Success -> {
                        response.value.forEachIndexed { index, feedRecommendation ->
                            results.add(
                                FeedRecommendation(
                                    id = feedRecommendation.ref_id,
                                    feedType = feedRecommendation.type,
                                    description = feedRecommendation.description,
                                    smallImageUrl = feedRecommendation.s_image_url,
                                    mediumImageUrl = feedRecommendation.m_image_url,
                                    largeImageUrl = feedRecommendation.l_image_url,
                                    link = feedRecommendation.link,
                                    title = feedRecommendation.episode_title,
                                    showTitle = feedRecommendation.show_title,
                                    date = feedRecommendation.date,
                                    timestamp = feedRecommendation.timestamp,
                                    topics = feedRecommendation.topics,
                                    guests = feedRecommendation.guests,
                                    position = index + 1
                                )
                            )
                        }
                    }
                }
            }
        }.join()

        recommendationsPodcast.value = mapRecommendationsPodcast(results)

        emit(results)
    }

    private val feedRecommendationPodcastPresenterMapper: FeedRecommendationPodcastPresenterMapper by lazy {
        FeedRecommendationPodcastPresenterMapper()
    }

    private fun mapRecommendationsPodcast(
        recommendations: List<FeedRecommendation>
    ): Podcast? {
        val podcast = feedRecommendationPodcastPresenterMapper.getRecommendationsPodcast()

        podcast.episodes = recommendations.map {
            feedRecommendationPodcastPresenterMapper.mapFrom(
                it,
                podcast.id
            )
        }

        if (podcast.episodes.isEmpty()) {
            return null
        }

        return podcast
    }

    private suspend fun mapFeedDboList(
        listFeedDbo: List<FeedDbo>,
        queries: SphinxDatabaseQueries
    ): List<Feed> {
        val itemsMap: MutableMap<FeedId, ArrayList<FeedItem>> =
            LinkedHashMap(listFeedDbo.size)

        val chatsMap: MutableMap<ChatId, Chat?> =
            LinkedHashMap(listFeedDbo.size)

        for (dbo in listFeedDbo) {
            itemsMap[dbo.id] = ArrayList(0)
            chatsMap[dbo.chat_id] = null
        }

        itemsMap.keys.chunked(500).forEach { chunkedIds ->
            queries.feedItemsGetByFeedIds(chunkedIds)
                .executeAsList()
                .let { response ->
                    response.forEach { dbo ->
                        dbo.feed_id?.let { feedId ->
                            itemsMap[feedId]?.add(
                                feedItemDboPresenterMapper.mapFrom(dbo)
                            )
                        }
                    }
                }
        }

        chatsMap.keys.chunked(500).forEach { chunkedChatIds ->
            queries.chatGetAllByIds(chunkedChatIds)
                .executeAsList()
                .let { response ->
                    response.forEach { dbo ->
                        dbo.id?.let { chatId ->
                            chatsMap[chatId] = chatDboPresenterMapper.mapFrom(dbo)
                        }
                    }
                }
        }

        val list = listFeedDbo.map {
            mapFeedDbo(
                feedDbo = it,
                items = itemsMap[it.id] ?: listOf(),
                model = null,
                destinations = listOf(),
                chat = chatsMap[it.chat_id]
            )
        }

        var sortedList: List<Feed>? = null

        withContext(dispatchers.default) {
            sortedList = list.sortedByDescending {
                it.chat?.contentSeenAt?.time ?: it.lastItem?.datePublished?.time ?: 0
            }
        }

        return sortedList ?: listOf()
    }

    private suspend fun mapFeedDbo(
        feedDbo: FeedDbo,
        items: List<FeedItem>,
        model: FeedModel? = null,
        destinations: List<FeedDestination>,
        chat: Chat? = null,
    ): Feed {

        val feed = feedDboPresenterMapper.mapFrom(feedDbo)

        items.forEach { feedItem ->
            feedItem.feed = feed
        }

        feed.items = items
        feed.model = model
        feed.destinations = destinations
        feed.chat = chat

        return feed
    }

    private suspend fun mapFeedDbo(
        feed: Feed,
        queries: SphinxDatabaseQueries
    ): Feed {

        val model = queries.feedModelGetById(feed.id).executeAsOneOrNull()?.let { feedModelDbo ->
            feedModelDboPresenterMapper.mapFrom(feedModelDbo)
        }

        val chat = queries.chatGetById(feed.chatId).executeAsOneOrNull()?.let { chatDbo ->
            chatDboPresenterMapper.mapFrom(chatDbo)
        }

        val items = queries.feedItemsGetByFeedId(feed.id).executeAsList().map {
            feedItemDboPresenterMapper.mapFrom(it)
        }

        val destinations = queries.feedDestinationsGetByFeedId(feed.id).executeAsList().map {
            feedDestinationDboPresenterMapper.mapFrom(it)
        }

        items.forEach { feedItem ->
            feedItem.feed = feed
        }

        feed.items = items
        feed.model = model
        feed.destinations = destinations
        feed.chat = chat

        return feed
    }

    private val podcastDboPresenterMapper: FeedDboPodcastPresenterMapper by lazy {
        FeedDboPodcastPresenterMapper(dispatchers)
    }
    private val podcastDestinationDboPresenterMapper: FeedDestinationDboPodcastDestinationPresenterMapper by lazy {
        FeedDestinationDboPodcastDestinationPresenterMapper(dispatchers)
    }
    private val podcastEpisodeDboPresenterMapper: FeedItemDboPodcastEpisodePresenterMapper by lazy {
        FeedItemDboPodcastEpisodePresenterMapper(dispatchers)
    }
    private val podcastModelDboPresenterMapper: FeedModelDboPodcastModelPresenterMapper by lazy {
        FeedModelDboPodcastModelPresenterMapper(dispatchers)
    }

    override fun getPodcastByChatId(chatId: ChatId): Flow<Podcast?> = flow {
        val queries = coreDB.getSphinxDatabaseQueries()

        queries.feedGetByChatIdAndType(chatId, FeedType.Podcast)
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.let { podcastDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
            .collect { value: Podcast? ->
                value?.let { podcast ->
                    emit(
                        processPodcast(podcast, queries)
                    )
                }
            }
    }

    override fun getPodcastById(feedId: FeedId): Flow<Podcast?> = flow {
        if (feedId.value == FeedRecommendation.RECOMMENDATION_PODCAST_ID) {
            emit(recommendationsPodcast.value)
            return@flow
        }

        val queries = coreDB.getSphinxDatabaseQueries()

        queries.feedGetById(feedId)
            .asFlow()
            .mapToOneOrNull(io)
            .map { it?.let { podcastDboPresenterMapper.mapFrom(it) } }
            .distinctUntilChanged()
            .collect { value: Podcast? ->
                value?.let { podcast ->
                    emit(
                        processPodcast(podcast, queries)
                    )
                }
            }
    }

    private suspend fun processPodcast(
        podcast: Podcast,
        queries: SphinxDatabaseQueries
    ): Podcast {

        queries.feedModelGetById(podcast.id).executeAsOneOrNull()?.let { feedModelDbo ->
            podcast.model = podcastModelDboPresenterMapper.mapFrom(feedModelDbo)
        }

        val episodes = queries.feedItemsGetByFeedId(podcast.id).executeAsList().map {
            podcastEpisodeDboPresenterMapper.mapFrom(it)
        }

        val destinations = queries.feedDestinationsGetByFeedId(podcast.id).executeAsList().map {
            podcastDestinationDboPresenterMapper.mapFrom(it)
        }

        podcast.episodes = episodes
        podcast.destinations = destinations

        return podcast
    }

    private val feedSearchResultDboPresenterMapper: FeedDboFeedSearchResultPresenterMapper by lazy {
        FeedDboFeedSearchResultPresenterMapper(dispatchers)
    }

    private suspend fun getSubscribedItemsBy(
        searchTerm: String,
        feedType: FeedType?
    ): MutableList<FeedSearchResultRow> {
        val queries = coreDB.getSphinxDatabaseQueries()
        var results: MutableList<FeedSearchResultRow> = mutableListOf()

        val subscribedItems = if (feedType == null) {
            queries
                .feedGetAllByTitle("%${searchTerm.lowercase().trim()}%")
                .executeAsList()
                .map { it?.let { feedSearchResultDboPresenterMapper.mapFrom(it) } }
        } else {
            queries
                .feedGetAllByTitleAndType("%${searchTerm.lowercase().trim()}%", feedType)
                .executeAsList()
                .map { it?.let { feedSearchResultDboPresenterMapper.mapFrom(it) } }
        }


        if (subscribedItems.count() > 0) {
            results.add(
                FeedSearchResultRow(
                    feedSearchResult = null,
                    isSectionHeader = true,
                    isFollowingSection = true,
                    isLastOnSection = false
                )
            )

            subscribedItems.forEachIndexed { index, item ->
                results.add(
                    FeedSearchResultRow(
                        item,
                        isSectionHeader = false,
                        isFollowingSection = true,
                        (index == subscribedItems.count() - 1)
                    )
                )
            }
        }

        return results
    }

    override fun searchFeedsBy(
        searchTerm: String,
        feedType: FeedType?,
    ): Flow<List<FeedSearchResultRow>> = flow {
        if (feedType == null) {
            emit(
                getSubscribedItemsBy(searchTerm, feedType)
            )
            return@flow
        }

        var results: MutableList<FeedSearchResultRow> = mutableListOf()

        networkQueryFeedSearch.searchFeeds(
            searchTerm,
            feedType
        ).collect { response ->
            @Exhaustive
            when (response) {
                is LoadResponse.Loading -> {}

                is Response.Error -> {
                    results.addAll(
                        getSubscribedItemsBy(searchTerm, feedType)
                    )
                }
                is Response.Success -> {

                    results.addAll(
                        getSubscribedItemsBy(searchTerm, feedType)
                    )

                    if (response.value.count() > 0) {
                        results.add(
                            FeedSearchResultRow(
                                feedSearchResult = null,
                                isSectionHeader = true,
                                isFollowingSection = false,
                                isLastOnSection = false
                            )
                        )

                        response.value.forEachIndexed { index, item ->
                            results.add(
                                FeedSearchResultRow(
                                    item.toFeedSearchResult(),
                                    isSectionHeader = false,
                                    isFollowingSection = false,
                                    (index == response.value.count() - 1)
                                )
                            )
                        }
                    }
                }
            }
        }

        emit(results)
    }

    override suspend fun toggleFeedSubscribeState(
        feedId: FeedId,
        currentSubscribeState: Subscribed
    ) {
        val queries = coreDB.getSphinxDatabaseQueries()

        queries.feedUpdateSubscribe(
            if (currentSubscribeState.isTrue()) Subscribed.False else Subscribed.True,
            feedId
        )
    }

    private suspend fun mapFeedItemDboList(
        listFeedItemDbo: List<FeedItemDbo>,
        queries: SphinxDatabaseQueries
    ): List<FeedItem> {
        val feedsMap: MutableMap<FeedId, ArrayList<Feed>> =
            LinkedHashMap(listFeedItemDbo.size)

        for (dbo in listFeedItemDbo) {
            feedsMap[dbo.feed_id] = ArrayList(0)
        }

        feedsMap.keys.chunked(500).forEach { chunkedFeedIds ->
            queries.feedGetAllByIds(chunkedFeedIds)
                .executeAsList()
                .let { response ->
                    response.forEach { dbo ->
                        feedsMap[dbo.id]?.add(
                            feedDboPresenterMapper.mapFrom(dbo)
                        )
                    }
                }
        }

        return listFeedItemDbo.map {
            feedItemDboPresenterMapper.mapFrom(it).apply {
                it.feed_id
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

    override val networkRefreshMessages: Flow<LoadResponse<RestoreProgress, ResponseError>> by lazy {
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

            val restoring: Boolean = lastSeenMessagesDate == null

            val now: String = DateTime.nowUTC()

            val supervisor = SupervisorJob(currentCoroutineContext().job)
            val scope = CoroutineScope(supervisor)

            var networkResponseError: Response.Error<ResponseError>? = null

            val jobList =
                ArrayList<Job>(MESSAGE_PAGINATION_LIMIT * 2 /* MessageDto fields to potentially decrypt */)

            val latestMessageMap =
                mutableMapOf<ChatId, MessageDto>()

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
                            val messagesTotal = response.value.new_messages_total ?: 0

                            if (restoring && messagesTotal > 0) {

                                val restoreProgress = getMessagesRestoreProgress(
                                    messagesTotal,
                                    offset
                                )

                                emit(
                                    Response.Success(restoreProgress)
                                )
                            }

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

                                applicationScope.launch(io) {

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

                                                for (dto in newMessages) {

                                                    val id: Long? = dto.chat_id

                                                    if (id != null &&
                                                        chatIds.contains(ChatId(id))
                                                    ) {

                                                        if (dto.updateChatDboLatestMessage) {
                                                            if (!latestMessageMap.containsKey(
                                                                    ChatId(
                                                                        id
                                                                    )
                                                                )
                                                            ) {
                                                                latestMessageMap[ChatId(id)] = dto
                                                            } else {
                                                                val lastMessage =
                                                                    latestMessageMap[ChatId(id)]
                                                                if (lastMessage == null ||
                                                                    dto.created_at.toDateTime().time > lastMessage.created_at.toDateTime().time
                                                                ) {

                                                                    latestMessageMap[ChatId(id)] =
                                                                        dto
                                                                }
                                                            }
                                                        }
                                                    }

                                                    upsertMessage(dto, queries)
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
                                offset == -1 -> {
                                }
                                newMessages.size >= MESSAGE_PAGINATION_LIMIT -> {
                                    offset += MESSAGE_PAGINATION_LIMIT

                                    if (lastSeenMessagesDate == null) {
                                        val resumePageNumber =
                                            (offset / MESSAGE_PAGINATION_LIMIT)
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

            } ?: applicationScope.launch(mainImmediate) {

                authenticationStorage.putString(
                    REPOSITORY_LAST_SEEN_MESSAGE_DATE,
                    now
                )

                if (lastSeenMessagesDate == null) {
                    authenticationStorage.removeString(REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE)
                    LOG.d(TAG, "Removing message restore page number")
                }

            }.join()

            emit(
                Response.Success(
                    RestoreProgress(
                        false,
                        100
                    )
                )
            )
        }
    }

    private fun getMessagesRestoreProgress(
        newMessagesTotal: Int,
        offset: Int
    ): RestoreProgress {

        val pages: Int = if (newMessagesTotal <= MESSAGE_PAGINATION_LIMIT) {
            1
        } else {
            newMessagesTotal / MESSAGE_PAGINATION_LIMIT
        }

        val contactsRestoreProgressTotal = 4
        val messagesRestoreProgressTotal = 96
        val currentPage: Int = offset / MESSAGE_PAGINATION_LIMIT
        val progress: Int =
            contactsRestoreProgressTotal + (currentPage * messagesRestoreProgressTotal / pages)

        return RestoreProgress(
            true,
            progress
        )
    }

    override suspend fun didCancelRestore() {
        val now = DateTime.getFormatRelay().format(
            Date(DateTime.getToday00().time)
        )

        authenticationStorage.putString(
            REPOSITORY_LAST_SEEN_MESSAGE_DATE,
            now
        )

        authenticationStorage.removeString(REPOSITORY_LAST_SEEN_MESSAGE_RESTORE_PAGE)
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

                    val decrypted = decryptMediaKey(
                        MediaKey(mediaKey)
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

    override fun createNewInvite(
        nickname: String,
        welcomeMessage: String
    ): Flow<LoadResponse<Any, ResponseError>> = flow {

        val queries = coreDB.getSphinxDatabaseQueries()

        var response: Response<Any, ResponseError>? = null

        emit(LoadResponse.Loading)

        applicationScope.launch(mainImmediate) {
            networkQueryContact.createNewInvite(nickname, welcomeMessage)
                .collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = loadResponse
                        }

                        is Response.Success -> {
                            contactLock.withLock {
                                withContext(io) {
                                    queries.transaction {
                                        updatedContactIds.add(ContactId(loadResponse.value.id))
                                        upsertContact(loadResponse.value, queries)
                                    }
                                }
                            }
                            response = Response.Success(true)
                        }
                    }
                }
        }.join()

        emit(response ?: Response.Error(ResponseError("")))
    }

    override suspend fun payForInvite(invite: Invite) {
        val queries = coreDB.getSphinxDatabaseQueries()

        contactLock.withLock {
            withContext(io) {
                queries.transaction {
                    updatedContactIds.add(invite.contactId)
                    updateInviteStatus(invite.id, InviteStatus.ProcessingPayment, queries)
                }
            }
        }

        delay(25L)
        networkQueryInvite.payInvite(invite.inviteString).collect { loadResponse ->
            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                }

                is Response.Error -> {
                    contactLock.withLock {
                        withContext(io) {
                            queries.transaction {
                                updatedContactIds.add(invite.contactId)
                                updateInviteStatus(
                                    invite.id,
                                    InviteStatus.PaymentPending,
                                    queries
                                )
                            }
                        }
                    }
                }

                is Response.Success -> {
                }
            }
        }
    }

    override suspend fun deleteInvite(invite: Invite): Response<Any, ResponseError> {
        val queries = coreDB.getSphinxDatabaseQueries()

        val response = networkQueryContact.deleteContact(invite.contactId)

        if (response is Response.Success) {
            contactLock.withLock {
                withContext(io) {
                    queries.transaction {
                        updatedContactIds.add(invite.contactId)
                        deleteContactById(invite.contactId, queries)
                    }
                }

            }
        }
        return response
    }

    override suspend fun authorizeStakwork(
        host: String,
        id: String,
        challenge: String
    ): Response<String, ResponseError> {
        var response: Response<String, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            networkQueryMemeServer.signChallenge(
                AuthenticationChallenge(challenge)
            ).collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }

                    is Response.Error -> {
                        response = loadResponse
                    }

                    is Response.Success -> {

                        val sig = loadResponse.value.sig
                        val publicKey = accountOwner.value?.nodePubKey?.value ?: ""
                        val urlString =
                            "https://auth.sphinx.chat/oauth_verify?id=$id&sig=$sig&pubkey=$publicKey"

                        response = Response.Success(urlString)
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError("Returned before completing"))
    }

    override suspend fun authorizeExternal(
        relayUrl: String,
        host: String,
        challenge: String
    ): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            networkQueryAuthorizeExternal.verifyExternal().collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }

                    is Response.Error -> {
                        response = loadResponse
                    }

                    is Response.Success -> {

                        val token = loadResponse.value.token
                        val info = loadResponse.value.info

                        networkQueryAuthorizeExternal.signBase64(
                            AUTHORIZE_EXTERNAL_BASE_64
                        ).collect { sigResponse ->

                            when (sigResponse) {
                                is LoadResponse.Loading -> {
                                }

                                is Response.Error -> {
                                    response = sigResponse
                                }

                                is Response.Success -> {

                                    info.verificationSignature = sigResponse.value.sig
                                    info.url = relayUrl

                                    networkQueryAuthorizeExternal.authorizeExternal(
                                        host,
                                        challenge,
                                        token,
                                        info,
                                    ).collect { authorizeResponse ->
                                        when (authorizeResponse) {
                                            is LoadResponse.Loading -> {
                                            }

                                            is Response.Error -> {
                                                response = authorizeResponse
                                            }

                                            is Response.Success -> {
                                                response = Response.Success(true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError("Returned before completing"))
    }

    override suspend fun deletePeopleProfile(
        body: String
    ): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            moshi.adapter(DeletePeopleProfileDto::class.java).fromJson(body)
                ?.let { deletePeopleProfileDto ->
                    networkQueryPeople.deletePeopleProfile(
                        deletePeopleProfileDto
                    ).collect { loadResponse ->
                        when (loadResponse) {
                            is LoadResponse.Loading -> {
                            }
                            is Response.Error -> {
                            }
                            is Response.Success -> {
                                response = Response.Success(true)
                            }
                        }
                    }
                }
        }.join()

        return response ?: Response.Error(ResponseError("Profile delete failed"))
    }

    override suspend fun savePeopleProfile(
        body: String
    ): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            moshi.adapter(PeopleProfileDto::class.java).fromJson(body)?.let { profile ->
                networkQueryPeople.savePeopleProfile(
                    profile
                ).collect { saveProfileResponse ->
                    when (saveProfileResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = saveProfileResponse
                        }

                        is Response.Success -> {
                            response = Response.Success(true)
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError("Profile save failed"))
    }

    override suspend fun redeemBadgeToken(
        body: String
    ): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            moshi.adapter(RedeemBadgeTokenDto::class.java).fromJson(body)?.let { profile ->
                networkQueryRedeemBadgeToken.redeemBadgeToken(
                    profile
                ).collect { redeemBadgeTokenResponse ->
                    when (redeemBadgeTokenResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = redeemBadgeTokenResponse
                        }

                        is Response.Success -> {
                            response = Response.Success(true)
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError("Redeem Badge Token failed"))
    }


    override suspend fun exitAndDeleteTribe(chat: Chat): Response<Boolean, ResponseError> {
        var response: Response<Boolean, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            networkQueryChat.deleteChat(chat.id).collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }

                    is Response.Error -> {
                        response = loadResponse
                    }

                    is Response.Success -> {
                        response = Response.Success(true)
                        val queries = coreDB.getSphinxDatabaseQueries()

                        chatLock.withLock {
                            messageLock.withLock {
                                withContext(io) {
                                    queries.transaction {
                                        deleteChatById(
                                            loadResponse.value["chat_id"]?.toChatId()
                                                ?: chat.id,
                                            queries,
                                            latestMessageUpdatedTimeMap
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError(("Failed to exit tribe")))
    }

    override suspend fun createTribe(createTribe: CreateTribe): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError> =
            Response.Error(ResponseError(("Failed to exit tribe")))
        val memeServerHost = MediaHost.DEFAULT

        applicationScope.launch(mainImmediate) {
            try {
                val imgUrl: String? = createTribe.img?.let { imgFile ->
                    // If an image file is provided we should upload it
                    val token =
                        memeServerTokenHandler.retrieveAuthenticationToken(memeServerHost)
                            ?: throw RuntimeException("MemeServerAuthenticationToken retrieval failure")

                    val networkResponse = networkQueryMemeServer.uploadAttachment(
                        authenticationToken = token,
                        mediaType = MediaType.Image("${MediaType.IMAGE}/${imgFile.extension}"),
                        stream = object : InputStreamProvider() {
                            override fun newInputStream(): InputStream = imgFile.inputStream()
                        },
                        fileName = imgFile.name,
                        contentLength = imgFile.length(),
                        memeServerHost = memeServerHost,
                    )
                    @Exhaustive
                    when (networkResponse) {
                        is Response.Error -> {
                            LOG.e(TAG, "Failed to upload image: ", networkResponse.exception)
                            response = networkResponse
                            null
                        }
                        is Response.Success -> {
                            "https://${memeServerHost.value}/public/${networkResponse.value.muid}"
                        }
                    }
                }

                networkQueryChat.createTribe(
                    createTribe.toPostGroupDto(imgUrl)
                ).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = loadResponse
                            LOG.e(TAG, "Failed to create tribe: ", loadResponse.exception)
                        }
                        is Response.Success -> {
                            loadResponse.value?.let { chatDto ->
                                response = Response.Success(chatDto)
                                val queries = coreDB.getSphinxDatabaseQueries()

                                chatLock.withLock {
                                    messageLock.withLock {
                                        withContext(io) {
                                            queries.transaction {
                                                upsertChat(
                                                    chatDto,
                                                    moshi,
                                                    chatSeenMap,
                                                    queries,
                                                    null,
                                                    accountOwner.value?.nodePubKey
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                response = Response.Error(
                    ResponseError("Failed to update Chat Profile", e)
                )
            }
        }.join()

        return response
    }

    override suspend fun updateTribe(
        chatId: ChatId,
        createTribe: CreateTribe
    ): Response<Any, ResponseError> {

        var response: Response<Any, ResponseError> =
            Response.Error(ResponseError(("Failed to exit tribe")))
        val memeServerHost = MediaHost.DEFAULT

        applicationScope.launch(mainImmediate) {
            try {
                val imgUrl: String? = (createTribe.img?.let { imgFile ->
                    val token =
                        memeServerTokenHandler.retrieveAuthenticationToken(memeServerHost)
                            ?: throw RuntimeException("MemeServerAuthenticationToken retrieval failure")

                    val networkResponse = networkQueryMemeServer.uploadAttachment(
                        authenticationToken = token,
                        mediaType = MediaType.Image("${MediaType.IMAGE}/${imgFile.extension}"),
                        stream = object : InputStreamProvider() {
                            override fun newInputStream(): InputStream = imgFile.inputStream()
                        },
                        fileName = imgFile.name,
                        contentLength = imgFile.length(),
                        memeServerHost = memeServerHost,
                    )
                    @Exhaustive
                    when (networkResponse) {
                        is Response.Error -> {
                            LOG.e(TAG, "Failed to upload image: ", networkResponse.exception)
                            response = networkResponse
                            null
                        }
                        is Response.Success -> {
                            "https://${memeServerHost.value}/public/${networkResponse.value.muid}"
                        }
                    }
                }) ?: createTribe.imgUrl

                networkQueryChat.updateTribe(
                    chatId,
                    createTribe.toPostGroupDto(imgUrl)
                ).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = loadResponse
                            LOG.e(TAG, "Failed to create tribe: ", loadResponse.exception)
                        }
                        is Response.Success -> {
                            response = Response.Success(loadResponse.value)
                            val queries = coreDB.getSphinxDatabaseQueries()

                            chatLock.withLock {
                                messageLock.withLock {
                                    withContext(io) {
                                        queries.transaction {
                                            upsertChat(
                                                loadResponse.value,
                                                moshi,
                                                chatSeenMap,
                                                queries,
                                                null,
                                                accountOwner.value?.nodePubKey
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                response = Response.Error(
                    ResponseError("Failed to update Chat Profile", e)
                )
            }
        }.join()

        return response
    }

    override suspend fun processMemberRequest(
        contactId: ContactId,
        messageId: MessageId,
        type: MessageType,
    ): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError> = Response.Error(ResponseError(("")))

        applicationScope.launch(mainImmediate) {
            networkQueryMessage.processMemberRequest(
                contactId,
                messageId,
                type
            ).collect { loadResponse ->

                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }

                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        chatLock.withLock {
                            messageLock.withLock {
                                withContext(io) {
                                    queries.transaction {
                                        upsertChat(
                                            loadResponse.value.chat,
                                            moshi,
                                            chatSeenMap,
                                            queries,
                                            null,
                                            accountOwner.value?.nodePubKey
                                        )

                                        upsertMessage(loadResponse.value.message, queries)

                                        updateChatDboLatestMessage(
                                            loadResponse.value.message,
                                            ChatId(loadResponse.value.chat.id),
                                            latestMessageUpdatedTimeMap,
                                            queries,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response
    }

    override suspend fun addTribeMember(addMember: AddMember): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError> =
            Response.Error(ResponseError(("Failed to add Member")))
        val memeServerHost = MediaHost.DEFAULT

        applicationScope.launch(mainImmediate) {
            try {
                val imgUrl: String? = addMember.img?.let { imgFile ->
                    // If an image file is provided we should upload it
                    val token = memeServerTokenHandler.retrieveAuthenticationToken(memeServerHost)
                        ?: throw RuntimeException("MemeServerAuthenticationToken retrieval failure")

                    val networkResponse = networkQueryMemeServer.uploadAttachment(
                        authenticationToken = token,
                        mediaType = MediaType.Image("${MediaType.IMAGE}/${imgFile.extension}"),
                        stream = object : InputStreamProvider() {
                            override fun newInputStream(): InputStream = imgFile.inputStream()
                        },
                        fileName = imgFile.name,
                        contentLength = imgFile.length(),
                        memeServerHost = memeServerHost,
                    )
                    @Exhaustive
                    when (networkResponse) {
                        is Response.Error -> {
                            LOG.e(TAG, "Failed to upload image: ", networkResponse.exception)
                            response = networkResponse
                            null
                        }
                        is Response.Success -> {
                            "https://${memeServerHost.value}/public/${networkResponse.value.muid}"
                        }
                    }
                }

                networkQueryChat.addTribeMember(
                    addMember.toTribeMemberDto(imgUrl)
                ).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = loadResponse
                            LOG.e(TAG, "Failed to create tribe: ", loadResponse.exception)
                        }
                        is Response.Success -> {
                            response = Response.Success(true)
                        }
                    }
                }
            } catch (e: Exception) {
                response = Response.Error(
                    ResponseError("Failed to add Tribe Member", e)
                )
            }
        }.join()

        return response
    }

    override suspend fun kickMemberFromTribe(
        chatId: ChatId,
        contactId: ContactId
    ): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError> =
            Response.Error(ResponseError(("Failed to kick member from tribe")))

        applicationScope.launch(mainImmediate) {
            networkQueryChat.kickMemberFromChat(
                chatId,
                contactId
            ).collect { loadResponse ->

                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }

                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        chatLock.withLock {
                            messageLock.withLock {
                                withContext(io) {
                                    queries.transaction {
                                        upsertChat(
                                            loadResponse.value,
                                            moshi,
                                            chatSeenMap,
                                            queries,
                                            null,
                                            accountOwner.value?.nodePubKey
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response
    }

    /***
     * Subscriptions
     */

    private val subscriptionLock = Mutex()
    private val subscriptionDboPresenterMapper: SubscriptionDboPresenterMapper by lazy {
        SubscriptionDboPresenterMapper(dispatchers)
    }

    override fun getActiveSubscriptionByContactId(contactId: ContactId): Flow<Subscription?> =
        flow {
            emitAll(
                coreDB.getSphinxDatabaseQueries()
                    .subscriptionGetLastActiveByContactId(contactId)
                    .asFlow()
                    .mapToOneOrNull(io)
                    .map { it?.let { subscriptionDboPresenterMapper.mapFrom(it) } }
                    .distinctUntilChanged()
            )
        }

    override suspend fun createSubscription(
        amount: Sat,
        interval: String,
        contactId: ContactId,
        chatId: ChatId?,
        endDate: String?,
        endNumber: EndNumber?
    ): Response<Any, ResponseError> {
        var response: Response<SubscriptionDto, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            networkQuerySubscription.postSubscription(
                PostSubscriptionDto(
                    amount = amount.value,
                    contact_id = contactId.value,
                    chat_id = chatId?.value,
                    interval = interval,
                    end_number = endNumber?.value,
                    end_date = endDate
                )
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        subscriptionLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertSubscription(
                                        loadResponse.value,
                                        queries
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError(("Failed to create subscription")))
    }

    override suspend fun updateSubscription(
        id: SubscriptionId,
        amount: Sat,
        interval: String,
        contactId: ContactId,
        chatId: ChatId?,
        endDate: String?,
        endNumber: EndNumber?
    ): Response<Any, ResponseError> {
        var response: Response<SubscriptionDto, ResponseError>? = null

        applicationScope.launch(mainImmediate) {

            networkQuerySubscription.putSubscription(
                id,
                PutSubscriptionDto(
                    amount = amount.value,
                    contact_id = contactId.value,
                    chat_id = chatId?.value,
                    interval = interval,
                    end_number = endNumber?.value,
                    end_date = endDate
                )
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        subscriptionLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertSubscription(
                                        loadResponse.value,
                                        queries
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError(("Failed to update subscription")))
    }

    override suspend fun restartSubscription(
        subscriptionId: SubscriptionId
    ): Response<Any, ResponseError> {
        var response: Response<SubscriptionDto, ResponseError>? = null

        applicationScope.launch(mainImmediate) {

            networkQuerySubscription.putRestartSubscription(
                subscriptionId
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        subscriptionLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertSubscription(
                                        loadResponse.value,
                                        queries
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError(("Failed to restart subscription")))
    }

    override suspend fun pauseSubscription(
        subscriptionId: SubscriptionId
    ): Response<Any, ResponseError> {
        var response: Response<SubscriptionDto, ResponseError>? = null

        applicationScope.launch(mainImmediate) {

            networkQuerySubscription.putPauseSubscription(
                subscriptionId
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        subscriptionLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    upsertSubscription(
                                        loadResponse.value,
                                        queries
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError(("Failed to pause subscription")))
    }

    override suspend fun deleteSubscription(
        subscriptionId: SubscriptionId
    ): Response<Any, ResponseError> {
        var response: Response<Any, ResponseError>? = null

        applicationScope.launch(mainImmediate) {
            networkQuerySubscription.deleteSubscription(
                subscriptionId
            ).collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                    }
                    is Response.Error -> {
                        response = loadResponse
                    }
                    is Response.Success -> {
                        response = loadResponse
                        val queries = coreDB.getSphinxDatabaseQueries()

                        subscriptionLock.withLock {
                            withContext(io) {
                                queries.transaction {
                                    deleteSubscriptionById(subscriptionId, queries)
                                }
                            }
                        }
                    }
                }
            }
        }.join()

        return response ?: Response.Error(ResponseError(("Failed to delete subscription")))
    }

    private val downloadMessageMediaLockMap = SynchronizedMap<MessageId, Pair<Int, Mutex>>()
    override fun downloadMediaIfApplicable(
        message: Message,
        sent: Boolean
    ) {
        applicationScope.launch(mainImmediate) {
            val messageId: MessageId = message.id

            val downloadLock: Mutex = downloadMessageMediaLockMap.withLock { map ->
                val localLock: Pair<Int, Mutex>? = map[messageId]

                if (localLock != null) {
                    map[messageId] = Pair(localLock.first + 1, localLock.second)
                    localLock.second
                } else {
                    Pair(1, Mutex()).let { pair ->
                        map[messageId] = pair
                        pair.second
                    }
                }
            }

            downloadLock.withLock {
                val queries = coreDB.getSphinxDatabaseQueries()

                //Getting media data from purchase accepted item if is paid content
                val media = message?.retrieveUrlAndMessageMedia()?.second
                val host = media?.host
                val url = media?.url

                val localFile = message?.messageMedia?.localFile

                if (
                    message != null &&
                    media != null &&
                    host != null &&
                    url != null &&
                    localFile == null &&
                    !message.status.isDeleted() &&
                    (!message.isPaidPendingMessage || sent)
                ) {

                    memeServerTokenHandler.retrieveAuthenticationToken(host)?.let { token ->
                        memeInputStreamHandler.retrieveMediaInputStream(
                            url.value,
                            token,
                            media.mediaKeyDecrypted,
                        )?.let { streamAndFileName ->

                            mediaCacheHandler.createFile(
                                mediaType = message.messageMedia?.mediaType ?: media.mediaType,
                                extension = streamAndFileName.second?.getExtension()
                            )?.let { streamToFile ->

                                streamAndFileName.first?.let { stream ->
                                    mediaCacheHandler.copyTo(stream, streamToFile)
                                    messageLock.withLock {
                                        withContext(io) {
                                            queries.transaction {

                                                queries.messageMediaUpdateFile(
                                                    streamToFile,
                                                    streamAndFileName.second,
                                                    messageId
                                                )

                                                // to proc table change so new file path is pushed to UI
                                                queries.messageUpdateContentDecrypted(
                                                    message.messageContentDecrypted,
                                                    messageId
                                                )
                                            }
                                        }
                                    }

                                    // hold downloadLock until table change propagates to UI
                                    delay(200L)

                                }
                            }
                        }
                    }
                }

                // remove lock from map if only subscriber
                downloadMessageMediaLockMap.withLock { map ->
                    map[messageId]?.let { pair ->
                        if (pair.first <= 1) {
                            map.remove(messageId)
                        } else {
                            map[messageId] = Pair(pair.first - 1, pair.second)
                        }
                    }
                }
            }
        }
    }

    private val feedItemLock = Mutex()
    private val downloadFeedItemLockMap = SynchronizedMap<FeedId, Pair<Int, Mutex>>()

    override fun inProgressDownloadIds(): List<FeedId> {
        return downloadFeedItemLockMap.withLock { map ->
            map.keys.toList()
        }
    }

    override fun downloadMediaIfApplicable(
        feedItem: DownloadableFeedItem,
        downloadCompleteCallback: (downloadedFile: File) -> Unit
    ) {
        val feedItemId: FeedId = feedItem.id

        val downloadLock: Mutex = downloadFeedItemLockMap.withLock { map ->
            val localLock: Pair<Int, Mutex>? = map[feedItemId]

            if (localLock != null) {
                map[feedItemId] = Pair(localLock.first + 1, localLock.second)
                localLock.second
            } else {
                Pair(1, Mutex()).let { pair ->
                    map[feedItemId] = pair
                    pair.second
                }
            }
        }

        applicationScope.launch(mainImmediate) {
            downloadLock.withLock {
                sphinxNotificationManager.notify(
                    notificationId = SphinxNotificationManager.DOWNLOAD_NOTIFICATION_ID,
                    title = "Downloading Item",
                    message = "Downloading item for local playback",
                )

                val queries = coreDB.getSphinxDatabaseQueries()

                val url = feedItem.enclosureUrl.value
                val contentType = feedItem.enclosureType
                val localFile = feedItem.localFile

                if (
                    contentType != null &&
                    localFile == null
                ) {
                    val streamToFile: File? = mediaCacheHandler.createFile(
                        contentType.value.toMediaType()
                    )

                    if (streamToFile != null) {
                        memeInputStreamHandler.retrieveMediaInputStream(
                            url,
                            authenticationToken = null,
                            mediaKeyDecrypted = null,
                        )?.let { streamAndFileName ->
                            streamAndFileName.first?.let { stream ->
                                sphinxNotificationManager.notify(
                                    notificationId = SphinxNotificationManager.DOWNLOAD_NOTIFICATION_ID,
                                    title = "Completing Download",
                                    message = "Finishing up download of file",
                                )
                                mediaCacheHandler.copyTo(stream, streamToFile)

                                feedItemLock.withLock {
                                    withContext(io) {
                                        queries.transaction {
                                            queries.feedItemUpdateLocalFile(
                                                streamToFile,
                                                feedItemId
                                            )
                                        }
                                    }
                                }

                                sphinxNotificationManager.notify(
                                    notificationId = SphinxNotificationManager.DOWNLOAD_NOTIFICATION_ID,
                                    title = "Download complete",
                                    message = "item can now be accessed offline",
                                )
                                // hold downloadLock until table change propagates to UI
                                delay(200L)
                                downloadCompleteCallback.invoke(streamToFile)

                            } ?: streamToFile.delete()

                        } ?: streamToFile.delete()
                    }
                } else {
                    val title = if (localFile != null) {
                        "Item already downloaded"
                    } else {
                        "Failed to initiate download"
                    }
                    val message = if (localFile != null) {
                        "You have already downloaded this item."
                    } else {
                        "Failed to initiate download because of missing media type information"
                    }
                    sphinxNotificationManager.notify(
                        notificationId = SphinxNotificationManager.DOWNLOAD_NOTIFICATION_ID,
                        title = title,
                        message = message,
                    )
                }

                // remove lock from map if only subscriber
                downloadFeedItemLockMap.withLock { map ->
                    map[feedItemId]?.let { pair ->
                        if (pair.first <= 1) {
                            map.remove(feedItemId)
                        } else {
                            map[feedItemId] = Pair(pair.first - 1, pair.second)
                        }
                    }
                }
            }
        }
    }


    override suspend fun deleteDownloadedMediaIfApplicable(
        feedItem: DownloadableFeedItem
    ): Boolean {
        val feedItemId: FeedId = feedItem.id
        val queries = coreDB.getSphinxDatabaseQueries()

        val localFile = feedItem.localFile

        localFile?.let {
            try {
                if (it.exists()) {
                    it.delete()
                }

                feedItemLock.withLock {
                    withContext(io) {
                        queries.transaction {
                            queries.feedItemUpdateLocalFile(
                                null,
                                feedItemId
                            )
                        }
                    }
                }
                delay(200L)

                return true
            } catch (e: Exception) {

            }
        }
        return false
    }

    override suspend fun getPaymentTemplates(): Response<List<PaymentTemplate>, ResponseError> {
        var response: Response<List<PaymentTemplate>, ResponseError>? = null

        val memeServerHost = MediaHost.DEFAULT

        memeServerTokenHandler.retrieveAuthenticationToken(memeServerHost)?.let { token ->
            networkQueryMemeServer.getPaymentTemplates(token, moshi = moshi)
                .collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {
                        }

                        is Response.Error -> {
                            response = loadResponse
                        }

                        is Response.Success -> {
                            var templates = ArrayList<PaymentTemplate>(loadResponse.value.size)

                            for (ptDto in loadResponse.value) {
                                templates.add(
                                    PaymentTemplate(
                                        ptDto.muid,
                                        ptDto.width,
                                        ptDto.height,
                                        token.value
                                    )
                                )
                            }

                            response = Response.Success(templates)
                        }
                    }
                }
        }

        return response ?: Response.Error(ResponseError(("Failed to load payment templates")))
    }

    override fun getAndSaveTransportKey() {
        applicationScope.launch(io) {
            relayDataHandler.retrieveRelayTransportKey()?.let {
                return@launch
            }
            relayDataHandler.retrieveRelayUrl()?.let { relayUrl ->
                networkQueryRelayKeys.getRelayTransportKey(
                    relayUrl
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {}
                        is Response.Success -> {
                            relayDataHandler.persistRelayTransportKey(
                                RsaPublicKey(
                                    loadResponse.value.transport_key.toCharArray()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(RawPasswordAccess::class, UnencryptedDataAccess::class)
    override fun getOrCreateHMacKey() {
        applicationScope.launch(io) {
            relayDataHandler.retrieveRelayHMacKey()?.let {
                return@launch
            }
            networkQueryRelayKeys.getRelayHMacKey().collect { loadResponse ->
                @Exhaustive
                when (loadResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        when (val hMacKeyResponse = createHMacKey()) {
                            is Response.Error -> {}
                            is Response.Success -> {
                                relayDataHandler.persistRelayHMacKey(
                                    hMacKeyResponse.value
                                )
                            }
                        }
                    }
                    is Response.Success -> {
                        val privateKey: CharArray = authenticationCoreManager.getEncryptionKey()
                            ?.privateKey
                            ?.value
                            ?: return@collect

                        val response = rsa.decrypt(
                            rsaPrivateKey = RsaPrivateKey(privateKey),
                            text = EncryptedString(loadResponse.value.encrypted_key),
                            dispatcher = default
                        )

                        when (response) {
                            is Response.Error -> {}
                            is Response.Success -> {
                                relayDataHandler.persistRelayHMacKey(
                                    RelayHMacKey(
                                        response.value.toUnencryptedString(trim = false).value
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun createHMacKey(): Response<RelayHMacKey, ResponseError> {
        var response: Response<RelayHMacKey, ResponseError> = Response.Error(
            ResponseError("HMac Key creation failed")
        )

        @OptIn(RawPasswordAccess::class)
        val hMacKeyString = PasswordGenerator(passwordLength = 20).password.value.joinToString("")

        relayDataHandler.retrieveRelayTransportKey()?.let { key ->

            val encryptionResponse = rsa.encrypt(
                key,
                UnencryptedString(hMacKeyString),
                formatOutput = false,
                dispatcher = default,
            )

            when (encryptionResponse) {
                is Response.Error -> {}
                is Response.Success -> {
                    networkQueryRelayKeys.addRelayHMacKey(
                        PostHMacKeyDto(encryptionResponse.value.value)
                    ).collect { loadResponse ->
                        @Exhaustive
                        when (loadResponse) {
                            is LoadResponse.Loading -> {}
                            is Response.Error -> {}
                            is Response.Success -> {
                                response = Response.Success(
                                    RelayHMacKey(hMacKeyString)
                                )
                            }
                        }
                    }
                }
            }
        }

        return response
    }

    private val actionTrackDboMessagePresenterMapper: ActionTrackDboMessagePresenterMapper by lazy {
        ActionTrackDboMessagePresenterMapper(dispatchers, moshi)
    }

    private val actionTrackDboFeedSearchPresenterMapper: ActionTrackDboFeedSearchPresenterMapper by lazy {
        ActionTrackDboFeedSearchPresenterMapper(dispatchers, moshi)
    }

    private val actionTrackDboContentBoostPresenterMapper: ActionTrackDboContentBoostPresenterMapper by lazy {
        ActionTrackDboContentBoostPresenterMapper(dispatchers, moshi)
    }

    private val actionTrackDboPodcastClipCommentPresenterMapper: ActionTrackDboPodcastClipCommentPresenterMapper by lazy {
        ActionTrackDboPodcastClipCommentPresenterMapper(dispatchers, moshi)
    }

    private val actionTrackDboContentConsumedPresenterMapper: ActionTrackDboContentConsumedPresenterMapper by lazy {
        ActionTrackDboContentConsumedPresenterMapper(dispatchers, moshi)
    }

    @Suppress("RemoveExplicitTypeArguments")
    override val recommendationsToggleStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow<Boolean>(false)
    }

    override fun setRecommendationsToggle(enabled: Boolean) {
        recommendationsToggleStateFlow.value = enabled
    }

    override fun trackFeedSearchAction(searchTerm: String) {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            val searchTermCount = queries.feedSearchGetCount(
                "%\"searchTerm\":\"$searchTerm\"%"
            ).executeAsOneOrNull() ?: 0

            val feedSearchAction = FeedSearchAction(
                searchTermCount + 1,
                searchTerm,
                Date().time
            )

            queries.actionTrackUpsert(
                ActionTrackType.FeedSearch,
                ActionTrackMetaData(feedSearchAction.toJson(moshi)),
                false.toActionTrackUploaded(),
                ActionTrackId(Long.MAX_VALUE)
            )
        }
    }

    override fun trackFeedBoostAction(
        boost: Long,
        feedItemId: FeedId,
        topics: ArrayList<String>
    ) {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            getFeedItemById(feedItemId).firstOrNull()?.let { feedItem ->
                getFeedById(feedItem.feedId).firstOrNull()?.let { feed ->
                    feedItem.feed = feed

                    val contentBoostAction = ContentBoostAction(
                        boost,
                        feed.id.value,
                        feed.feedType.value.toLong(),
                        feed.feedUrl.value,
                        feedItem.id.value,
                        feedItem.enclosureUrl.value,
                        feed.titleToShow,
                        feedItem.titleToShow,
                        feedItem.descriptionToShow,
                        topics,
                        feedItem.people,
                        feedItem.datePublishedTime,
                        Date().time
                    )

                    queries.actionTrackUpsert(
                        ActionTrackType.ContentBoost,
                        ActionTrackMetaData(contentBoostAction.toJson(moshi)),
                        false.toActionTrackUploaded(),
                        ActionTrackId(Long.MAX_VALUE)
                    )
                }
            }
        }
    }

    override fun trackPodcastClipComments(
        feedItemId: FeedId,
        timestamp: Long,
        topics: ArrayList<String>
    ) {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            getFeedItemById(feedItemId).firstOrNull()?.let { feedItem ->
                getFeedById(feedItem.feedId).firstOrNull()?.let { feed ->
                    feedItem.feed = feed

                    val podcastClipCommentAction = PodcastClipCommentAction(
                        feed.id.value,
                        feed.feedType.value.toLong(),
                        feed.feedUrl.value,
                        feedItem.id.value,
                        feedItem.enclosureUrl.value,
                        feed.titleToShow,
                        feedItem.titleToShow,
                        feedItem.descriptionToShow,
                        topics,
                        feedItem.people,
                        feedItem.datePublishedTime,
                        timestamp,
                        timestamp,
                        Date().time
                    )

                    queries.actionTrackUpsert(
                        ActionTrackType.PodcastClipComment,
                        ActionTrackMetaData(podcastClipCommentAction.toJson(moshi)),
                        false.toActionTrackUploaded(),
                        ActionTrackId(Long.MAX_VALUE)
                    )
                }
            }
        }
    }

    override fun trackNewsletterConsumed(feedItemId: FeedId) {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            getFeedItemById(feedItemId).firstOrNull()?.let { feedItem ->
                getFeedById(feedItem.feedId).firstOrNull()?.let { feed ->
                    feedItem.feed = feed

                    val newsletterConsumedAction = ContentConsumedAction(
                        feed.id.value,
                        feed.feedType.value.toLong(),
                        feed.feedUrl.value,
                        feedItem.id.value,
                        feedItem.enclosureUrl.value,
                        feed.titleToShow,
                        feedItem.titleToShow,
                        feedItem.descriptionToShow,
                        0,
                        arrayListOf(),
                        feedItem.people,
                        feedItem.datePublishedTime
                    )

                    val contentConsumedHistoryItem = ContentConsumedHistoryItem(
                        arrayListOf(""),
                        0,
                        0,
                        Date().time
                    )
                    newsletterConsumedAction.addHistoryItem(contentConsumedHistoryItem)

                    queries.actionTrackUpsert(
                        ActionTrackType.ContentConsumed,
                        ActionTrackMetaData(newsletterConsumedAction.toJson(moshi)),
                        false.toActionTrackUploaded(),
                        ActionTrackId(Long.MAX_VALUE)
                    )
                }
            }
        }
    }

    override fun trackMediaContentConsumed(
        feedItemId: FeedId,
        history: ArrayList<ContentConsumedHistoryItem>
    ) {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            getFeedItemById(feedItemId).firstOrNull()?.let { feedItem ->
                getFeedById(feedItem.feedId).firstOrNull()?.let { feed ->
                    feedItem.feed = feed

                    val contentConsumedAction = ContentConsumedAction(
                        feed.id.value,
                        feed.feedType.value.toLong(),
                        feed.feedUrl.value,
                        feedItem.id.value,
                        feedItem.enclosureUrl.value,
                        feed.titleToShow,
                        feedItem.titleToShow,
                        feedItem.descriptionToShow,
                        0,
                        arrayListOf(),
                        feedItem.people,
                        feedItem.datePublishedTime
                    )

                    contentConsumedAction.history = ArrayList(
                        history.filter {
                            (it.endTimestamp - it.startTimestamp) > 2000.toLong()
                        }
                    )

                    if (contentConsumedAction.history.isEmpty()) {
                        return@launch
                    }

                    queries.actionTrackUpsert(
                        ActionTrackType.ContentConsumed,
                        ActionTrackMetaData(contentConsumedAction.toJson(moshi)),
                        false.toActionTrackUploaded(),
                        ActionTrackId(Long.MAX_VALUE)
                    )
                }
            }
        }
    }

    override fun trackRecommendationsConsumed(
        feedItemId: FeedId,
        history: ArrayList<ContentConsumedHistoryItem>
    ) {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            recommendationsPodcast.value?.let { recommendationsPodcast ->
                recommendationsPodcast.getEpisodeWithId(feedItemId.value)?.let { recommendation ->
                    val clipRank = recommendationsPodcast.getItemRankForEpisodeWithId(feedItemId.value).toLong()

                    val contentConsumedAction = ContentConsumedAction(
                        recommendationsPodcast.id.value,
                        recommendation.longType,
                        recommendationsPodcast.feedUrl.value,
                        recommendation.id.value,
                        recommendation.enclosureUrl.value,
                        recommendation.showTitleToShow,
                        recommendation.titleToShow,
                        recommendation.descriptionToShow,
                        clipRank,
                        ArrayList(recommendation.topics),
                        ArrayList(recommendation.people),
                        recommendation.datePublishedTime
                    )

                    contentConsumedAction.history = ArrayList(
                        history.filter {
                            (it.endTimestamp - it.startTimestamp) > 2000.toLong()
                        }
                    )

                    if (contentConsumedAction.history.isEmpty()) {
                        return@launch
                    }

                    queries.actionTrackUpsert(
                        ActionTrackType.ContentConsumed,
                        ActionTrackMetaData(contentConsumedAction.toJson(moshi)),
                        false.toActionTrackUploaded(),
                        ActionTrackId(Long.MAX_VALUE)
                    )
                }
            }
        }
    }

    override fun trackMessageContent(keywords: List<String>) {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            val messageAction = MessageAction(
                ArrayList(keywords),
                Date().time
            )

            queries.actionTrackUpsert(
                ActionTrackType.Message,
                ActionTrackMetaData(messageAction.toJson(moshi)),
                false.toActionTrackUploaded(),
                ActionTrackId(Long.MAX_VALUE)
            )
        }
    }

    override fun syncActions() {
        if (!recommendationsToggleStateFlow.value) {
            return
        }
        applicationScope.launch(io) {
            val queries = coreDB.getSphinxDatabaseQueries()

            val actionsDboList = queries.actionTrackGetAllNotUploaded()
                .executeAsList()

            for (chunk in actionsDboList.chunked(50)) {
                val actionsIds = chunk.map { it.id }

                val actionTrackDTOs: MutableList<ActionTrackDto> = mutableListOf()

                chunk.forEach {
                    it.meta_data.value.toActionTrackMetaDataDtoOrNull(moshi)?.let { metaDataDto ->
                        actionTrackDTOs.add(
                            ActionTrackDto(
                                it.type.value,
                                metaDataDto
                            )
                        )
                    }
                }

                networkQueryActionTrack.sendActionsTracked(
                    SyncActionsDto(actionTrackDTOs)
                ).collect { response ->
                    when (response) {
                        is Response.Success -> {
                            queries.actionTrackUpdateUploadedItems(actionsIds)
                        }
                        is Response.Error -> {}
                        else -> {}
                    }
                }
            }
        }
    }

    override val appLogsStateFlow: MutableStateFlow<String> by lazy {
        MutableStateFlow("")
    }

    override fun setAppLog(log: String) {
        appLogsStateFlow.value = appLogsStateFlow.value + log + "\n"
    }
}
