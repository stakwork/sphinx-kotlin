package chat.sphinx.concept_network_query_chat

import chat.sphinx.concept_network_query_chat.model.*
import chat.sphinx.concept_network_query_chat.model.feed.FeedDto
import chat.sphinx.concept_network_query_chat.model.podcast.PodcastDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryChat {

    ///////////
    /// GET ///
    ///////////
    abstract fun getChats(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<List<ChatDto>, ResponseError>>

    abstract fun getTribeInfo(
        host: ChatHost,
        uuid: ChatUUID
    ): Flow<LoadResponse<TribeDto, ResponseError>>

    abstract fun getFeedContent(
        host: ChatHost,
        feedUrl: String
    ): Flow<LoadResponse<FeedDto, ResponseError>>

    abstract fun getPodcastFeed(
        host: ChatHost,
        feedUrl: String
    ): Flow<LoadResponse<PodcastDto, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
    abstract fun updateChat(
        chatId: ChatId,
        putChatDto: PutChatDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

//    app.put('/chat/:id', chats.addGroupMembers)
//    app.put('/member/:contactId/:status/:messageId', chatTribes.approveOrRejectMember)

    abstract fun kickMemberFromChat(
        chatId: ChatId,
        contactId: ContactId,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun updateTribe(
        chatId: ChatId,
        postGroupDto: PostGroupDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    ////////////
    /// POST ///
    ////////////
    abstract fun createTribe(
        postGroupDto: PostGroupDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto?, ResponseError>>

    abstract fun streamSats(
        postStreamSatsDto: PostStreamSatsDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<Any?, ResponseError>>

    abstract fun toggleMuteChat(
        chatId: ChatId,
        muted: ChatMuted,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun joinTribe(
        tribeDto: TribeDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    /**
     * Returns a map of "chat_id": chatId
     * */
    abstract suspend fun deleteChat(
        chatId: ChatId,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<Map<String, Long>, ResponseError>>
}
