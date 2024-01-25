package chat.sphinx.feature_network_query_chat

import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.*
import chat.sphinx.concept_network_query_chat.model.feed.FeedDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_chat.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_chat.NotificationLevel
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryChatImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryChat() {

    companion object {
        private const val ENDPOINT_CHAT = "/chat"
        private const val ENDPOINT_CHATS = "/chats"
        private const val ENDPOINT_EDIT_CHAT = "$ENDPOINT_CHATS/%d"
        private const val ENDPOINT_DELETE_CHAT = "$ENDPOINT_CHAT/%d"
        private const val ENDPOINT_MUTE_CHAT = "/chats/%d/%s"
        private const val ENDPOINT_NOTIFICATION_LEVEL = "/notify/%d/%d"
        private const val MUTE_CHAT = "mute"
        private const val UN_MUTE_CHAT = "unmute"
        private const val ENDPOINT_GROUP = "/group"
        private const val ENDPOINT_EDIT_GROUP = "/group/%d"
        private const val ENDPOINT_PIN_MESSAGE = "/chat_pin/%d"
        private const val ENDPOINT_KICK = "/kick/%d/%d"
        private const val ENDPOINT_MEMBER = "/member"
        private const val ENDPOINT_TRIBE = "/tribe"
        private const val ENDPOINT_STREAM_SATS = "/stream"
        private const val ENDPOINT_ADD_TRIBE_MEMBER = "/tribe_member"

        private const val GET_TRIBE_INFO_URL = "http://%s/tribes/%s"
        private const val GET_FEED_CONTENT_URL = "https://%s/feed?url=%s&fulltext=true"
    }

    ///////////
    /// GET ///
    ///////////
    private val getChatsFlowNullData: Flow<LoadResponse<List<ChatDto>, ResponseError>> by lazy {
        networkRelayCall.relayGet(
            responseJsonClass = GetChatsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CHATS,
            relayData = null
        )
    }

    override fun getChats(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<List<ChatDto>, ResponseError>> =
        if (relayData == null) {
            getChatsFlowNullData
        } else {
            networkRelayCall.relayGet(
                responseJsonClass = GetChatsRelayResponse::class.java,
                relayEndpoint = ENDPOINT_CHATS,
                relayData = relayData
            )
        }

    override fun getTribeInfo(
        host: ChatHost,
        tribePubKey: LightningNodePubKey
    ): Flow<LoadResponse<NewTribeDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(GET_TRIBE_INFO_URL, host.value, tribePubKey.value),
            responseJsonClass = NewTribeDto::class.java,
        )

    override fun getFeedContent(
        host: ChatHost,
        feedUrl: FeedUrl,
        chatUUID: ChatUUID?,
    ): Flow<LoadResponse<FeedDto, ResponseError>> =
        networkRelayCall.get(
            url = if (chatUUID != null) {
                "${String.format(GET_FEED_CONTENT_URL, host.value, feedUrl.value)}&uuid=${chatUUID.value}"
            } else {
                String.format(GET_FEED_CONTENT_URL, host.value, feedUrl.value)
            },
            responseJsonClass = FeedDto::class.java,
        )


    ///////////
    /// PUT ///
    ///////////
    override fun updateChat(
        chatId: ChatId,
        putChatDto: PutChatDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = UpdateChatRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_EDIT_CHAT, chatId.value),
            requestBodyJsonClass = PutChatDto::class.java,
            requestBody = putChatDto,
            relayData = relayData
        )

    override fun kickMemberFromChat(
        chatId: ChatId,
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>>  =
        networkRelayCall.relayPut(
            responseJsonClass = UpdateChatRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_KICK, chatId.value, contactId.value),
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

//    app.put('/chat/:id', chats.addGroupMembers)
//    app.put('/member/:contactId/:status/:messageId', chatTribes.approveOrRejectMember)
    override fun updateTribe(
        chatId: ChatId,
        postGroupDto: PostGroupDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = PostGroupRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_EDIT_GROUP, chatId.value),
            requestBodyJsonClass = PostGroupDto::class.java,
            requestBody = postGroupDto,
            relayData = relayData
        )

    override fun pinMessage(
        chatId: ChatId,
        putPinMessageDto: PutPinMessageDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<PutPinMessageDto, ResponseError>>  =
        networkRelayCall.relayPut(
            responseJsonClass = PinMessageRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_PIN_MESSAGE, chatId.value),
            requestBodyJsonClass = PutPinMessageDto::class.java,
            requestBody = putPinMessageDto,
            relayData = relayData
        )

    ////////////
    /// POST ///
    ////////////
    override fun createTribe(
        postGroupDto: PostGroupDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto?, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = PostGroupRelayResponse::class.java,
            relayEndpoint = ENDPOINT_GROUP,
            requestBodyJsonClass = PostGroupDto::class.java,
            requestBody = postGroupDto,
            relayData = relayData
        )

    override fun streamSats(
        postStreamSatsDto: PostStreamSatsDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = StreamSatsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_STREAM_SATS,
            requestBodyJsonClass = PostStreamSatsDto::class.java,
            requestBody = postStreamSatsDto,
            relayData = relayData
        )

    override fun toggleMuteChat(
        chatId: ChatId,
        muted: ChatMuted,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        toggleMuteChatImpl(
            endpoint = String.format(ENDPOINT_MUTE_CHAT, chatId.value, (if (muted.isTrue()) UN_MUTE_CHAT else MUTE_CHAT)),
            relayData = relayData
        )

    private fun toggleMuteChatImpl(
        endpoint: String,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = UpdateChatRelayResponse::class.java,
            relayEndpoint = endpoint,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    override fun setNotificationLevel(
        chatId: ChatId,
        notificationLevel: NotificationLevel,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = UpdateChatRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_NOTIFICATION_LEVEL, chatId.value, notificationLevel.value),
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    override fun joinTribe(
        tribeDto: TribeDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = JoinTribeRelayResponse::class.java,
            relayEndpoint = ENDPOINT_TRIBE,
            requestBodyJsonClass = TribeDto::class.java,
            requestBody = tribeDto,
            relayData = relayData
        )

    override suspend fun deleteChat(
        chatId: ChatId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Map<String, Long>, ResponseError>> =
        networkRelayCall.relayDelete(
            responseJsonClass = DeleteChatRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_DELETE_CHAT, chatId.value),
            requestBody = null,
            relayData = relayData,
        )

    override fun addTribeMember(
        memberDto: TribeMemberDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any?, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = AddTribeMemberRelayResponse::class.java,
            relayEndpoint = ENDPOINT_ADD_TRIBE_MEMBER,
            requestBodyJsonClass = TribeMemberDto::class.java,
            requestBody = memberDto,
            relayData = relayData
        )
}
