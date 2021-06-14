package chat.sphinx.concept_network_query_chat

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.concept_network_query_chat.model.PutTribeDto
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.ChatMuted
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
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

    abstract fun getPodcastFeed(
        host: ChatHost,
        feedUrl: String
    ): Flow<LoadResponse<PodcastDto, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
//    app.put('/chats/:id', chats.updateChat)
//    app.put('/chat/:id', chats.addGroupMembers)
//    app.put('/kick/:chat_id/:contact_id', chats.kickChatMember)
//    app.put('/member/:contactId/:status/:messageId', chatTribes.approveOrRejectMember)

    abstract fun updateTribe(
        chatId: ChatId,
        putTribeDto: PutTribeDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    ////////////
    /// POST ///
    ////////////
//    app.post('/group', chats.createGroupChat)\

    abstract fun toggleMuteChat(
        chatId: ChatId,
        muted: ChatMuted,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    abstract fun joinTribe(
        tribeDto: TribeDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChatDto, ResponseError>>

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/chat/:id', chats.deleteChat)
}
