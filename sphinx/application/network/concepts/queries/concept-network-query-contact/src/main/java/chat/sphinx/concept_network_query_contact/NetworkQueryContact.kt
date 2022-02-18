package chat.sphinx.concept_network_query_contact

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.contact.Blocked
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryContact {

    ///////////
    /// GET ///
    ///////////
    abstract fun getContacts(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<GetContactsResponse, ResponseError>>

    abstract fun getLatestContacts(
        date: DateTime?,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<GetLatestContactsResponse, ResponseError>>

    abstract fun getTribeMembers(
        chatId: ChatId,
        offset: Int = 0,
        limit: Int = 50,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<GetTribeMembersResponse, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
    abstract fun updateContact(
        contactId: ContactId,
        putContactDto: PutContactDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    abstract fun exchangeKeys(
        contactId: ContactId,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    abstract fun toggleBlockedContact(
        contactId: ContactId,
        blocked: Blocked,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    ////////////
    /// POST ///
    ////////////
    abstract fun generateToken(
        relayUrl: RelayUrl,
        token: AuthorizationToken,
        password: String?,
        pubkey: String? = null
    ): Flow<LoadResponse<GenerateTokenResponse, ResponseError>>

    abstract fun createContact(
        postContactDto: PostContactDto,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    //////////////
    /// DELETE ///
    //////////////
    abstract suspend fun deleteContact(
        contactId: ContactId,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Response<Any, ResponseError>

    abstract fun createNewInvite(
        nickname: String,
        welcomeMessage: String,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    //    app.post('/contacts/:id/keys', contacts.exchangeKeys)
    //    app.post('/contacts', contacts.createContact)
}
