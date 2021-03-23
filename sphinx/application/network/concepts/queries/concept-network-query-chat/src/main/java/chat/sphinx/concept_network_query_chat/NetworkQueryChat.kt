package chat.sphinx.concept_network_query_chat

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryChat {

    ///////////
    /// GET ///
    ///////////
    abstract fun getChats(): Flow<LoadResponse<List<ChatDto>, ResponseError>>

    abstract fun getChats(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<List<ChatDto>, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
//    app.put('/chats/:id', chats.updateChat)
//    app.put('/chat/:id', chats.addGroupMembers)
//    app.put('/kick/:chat_id/:contact_id', chats.kickChatMember)
//    app.put('/member/:contactId/:status/:messageId', chatTribes.approveOrRejectMember)
//    app.put('/group/:id', chatTribes.editTribe)

    ////////////
    /// POST ///
    ////////////
//    app.post('/group', chats.createGroupChat)
//    app.post('/chats/:chat_id/:mute_unmute', chats.mute)
//    app.post('/tribe', chatTribes.joinTribe)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/chat/:id', chats.deleteChat)
}
