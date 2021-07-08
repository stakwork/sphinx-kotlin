package chat.sphinx.feature_network_query_chat

import chat.sphinx.feature_network_query_chat.model.UpdateChatRelayResponse
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.*
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_chat.model.DeleteChatRelayResponse
import chat.sphinx.feature_network_query_chat.model.GetChatsRelayResponse
import chat.sphinx.feature_network_query_chat.model.JoinTribeRelayResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.*

class NetworkQueryChatImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryChat() {

    companion object {
        private const val ENDPOINT_CHAT = "/chat"
        private const val ENDPOINT_CHATS = "/chats"
        private const val ENDPOINT_EDIT_CHAT = "$ENDPOINT_CHATS/%d"
        private const val ENDPOINT_DELETE_CHAT = "$ENDPOINT_CHAT/%d"
        private const val ENDPOINT_MUTE_CHAT = "/chats/%d/%s"
        private const val MUTE_CHAT = "mute"
        private const val UN_MUTE_CHAT = "unmute"
        private const val ENDPOINT_GROUP = "/group"
        private const val ENDPOINT_EDIT_GROUP = "/group/%d"
        private const val ENDPOINT_KICK = "/kick"
        private const val ENDPOINT_MEMBER = "/member"
        private const val ENDPOINT_TRIBE = "/tribe"

        private const val GET_TRIBE_INFO_URL = "https://%s/tribes/%s"
        private const val GET_PODCAST_FEED_URL = "https://%s/podcast?url=%s"
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
        relayData: Pair<AuthorizationToken, RelayUrl>?
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
        uuid: ChatUUID
    ): Flow<LoadResponse<TribeDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(GET_TRIBE_INFO_URL, host.value, uuid.value),
            responseJsonClass = TribeDto::class.java,
        )

    override fun getPodcastFeed(
        host: ChatHost,
        feedUrl: String
    ): Flow<LoadResponse<PodcastDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(GET_PODCAST_FEED_URL, host.value, feedUrl),
            responseJsonClass = PodcastDto::class.java,
        )

    ///////////
    /// PUT ///
    ///////////
    override fun updateChat(
        chatId: ChatId,
        putChatDto: PutChatDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = UpdateChatRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_EDIT_CHAT, chatId.value),
            requestBodyJsonClass = PutChatDto::class.java,
            requestBody = putChatDto,
            relayData = relayData
        )

//    app.put('/chat/:id', chats.addGroupMembers)
//    app.put('/kick/:chat_id/:contact_id', chats.kickChatMember)
//    app.put('/member/:contactId/:status/:messageId', chatTribes.approveOrRejectMember)
    override fun updateTribe(
        chatId: ChatId,
        putTribeDto: PutTribeDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = UpdateChatRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_EDIT_GROUP, chatId.value),
            requestBodyJsonClass = PutTribeDto::class.java,
            requestBody = putTribeDto,
            relayData = relayData
        )

    ////////////
    /// POST ///
    ////////////
//    app.post('/group', chats.createGroupChat)

    override fun toggleMuteChat(
        chatId: ChatId,
        muted: ChatMuted,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        toggleMuteChatImpl(
            endpoint = String.format(ENDPOINT_MUTE_CHAT, chatId.value, (if (muted.isTrue()) UN_MUTE_CHAT else MUTE_CHAT)),
            relayData = relayData
        )

    private fun toggleMuteChatImpl(
        endpoint: String,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ChatDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = UpdateChatRelayResponse::class.java,
            relayEndpoint = endpoint,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    override fun joinTribe(
        tribeDto: TribeDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
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
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<DeleteChatResponseDto, ResponseError>> =
        networkRelayCall.relayDelete(
            DeleteChatRelayResponse::class.java,
            String.format(ENDPOINT_DELETE_CHAT, chatId.value),
            requestBody = null
        )
}
