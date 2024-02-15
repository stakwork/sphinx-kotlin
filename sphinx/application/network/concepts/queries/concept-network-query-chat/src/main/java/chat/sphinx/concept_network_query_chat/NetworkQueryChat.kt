package chat.sphinx.concept_network_query_chat

import chat.sphinx.concept_network_query_chat.model.*
import chat.sphinx.concept_network_query_chat.model.feed.FeedDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_chat.NotificationLevel
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

abstract class NetworkQueryChat {

    ///////////
    /// GET ///
    ///////////
    abstract fun getChats(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<List<ChatDto>, ResponseError>>

    abstract fun getTribeInfo(
        host: ChatHost,
        tribePubKey: LightningNodePubKey
    ): Flow<LoadResponse<NewTribeDto, ResponseError>>

    abstract fun getFeedContent(
        host: ChatHost,
        feedUrl: FeedUrl,
        chatUUID: ChatUUID?,
    ): Flow<LoadResponse<FeedDto, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
    abstract fun updateChat(
        chatId: ChatId,
        putChatDto: PutChatDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

//    app.put('/chat/:id', chats.addGroupMembers)
//    app.put('/member/:contactId/:status/:messageId', chatTribes.approveOrRejectMember)

    abstract fun kickMemberFromChat(
        chatId: ChatId,
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun updateTribe(
        chatId: ChatId,
        postGroupDto: PostGroupDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun pinMessage(
        chatId: ChatId,
        putPinMessageDto: PutPinMessageDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<PutPinMessageDto, ResponseError>>

    ////////////
    /// POST ///
    ////////////
    abstract fun createTribe(
        postGroupDto: PostGroupDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto?, ResponseError>>

    abstract fun streamSats(
        postStreamSatsDto: PostStreamSatsDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any?, ResponseError>>

    abstract fun toggleMuteChat(
        chatId: ChatId,
        muted: ChatMuted,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun setNotificationLevel(
        chatId: ChatId,
        notificationLevel: NotificationLevel,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun joinTribe(
        tribeDto: TribeDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun addTribeMember(
        memberDto: TribeMemberDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any?, ResponseError>>

    /**
     * Returns a map of "chat_id": chatId
     * */
    abstract suspend fun deleteChat(
        chatId: ChatId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Map<String, Long>, ResponseError>>
}
