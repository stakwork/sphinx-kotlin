package chat.sphinx.concept_network_query_contact

import chat.sphinx.concept_network_query_contact.model.*
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
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

    ///////////
    /// PUT ///
    ///////////
    abstract fun updateContact(
        contactId: ContactId,
        putContactDto: PutContactDto,
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
