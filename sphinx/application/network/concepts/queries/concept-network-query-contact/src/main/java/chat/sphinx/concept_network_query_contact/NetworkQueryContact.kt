package chat.sphinx.concept_network_query_contact

import chat.sphinx.concept_network_query_contact.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.contact.Blocked
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryContact {

    ///////////
    /// GET ///
    ///////////
    abstract fun getContacts(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<GetContactsResponse, ResponseError>>

    abstract fun getLatestContacts(
        date: DateTime?,
        limit: Int,
        offset: Int,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<GetLatestContactsResponse, ResponseError>>

    abstract fun getTribeMembers(
        chatId: ChatId,
        offset: Int = 0,
        limit: Int = 50,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<GetTribeMembersResponse, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
    abstract fun updateContact(
        contactId: ContactId,
        putContactDto: PutContactDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    abstract fun exchangeKeys(
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    abstract fun toggleBlockedContact(
        contactId: ContactId,
        blocked: Blocked,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    ////////////
    /// POST ///
    ////////////
    abstract fun generateToken(
        password: String?,
        publicKey: String? = null,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<GenerateTokenResponse, ResponseError>>

    abstract fun generateToken(
        relayUrl: RelayUrl,
        token: AuthorizationToken,
        password: String?,
        publicKey: String? = null
    ): Flow<LoadResponse<GenerateTokenResponse, ResponseError>>

    abstract fun createContact(
        postContactDto: PostContactDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    abstract fun generateGithubPAT(
        patDto: GithubPATDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

    //////////////
    /// DELETE ///
    //////////////
    abstract suspend fun deleteContact(
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null,
    ): Response<Any, ResponseError>

    abstract fun createNewInvite(
        nickname: String,
        welcomeMessage: String,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<ContactDto, ResponseError>>

    abstract fun hasAdmin(
        url: RelayUrl
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun deleteAccount(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

    //    app.post('/contacts/:id/keys', contacts.exchangeKeys)
    //    app.post('/contacts', contacts.createContact)
}
