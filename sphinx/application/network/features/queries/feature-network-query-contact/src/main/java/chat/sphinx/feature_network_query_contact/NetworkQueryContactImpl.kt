package chat.sphinx.feature_network_query_contact

import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.*
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_contact.model.ContactRelayResponse
import chat.sphinx.feature_network_query_contact.model.DeleteContactRelayResponse
import chat.sphinx.feature_network_query_contact.model.GenerateTokenRelayResponse
import chat.sphinx.feature_network_query_contact.model.GetContactsRelayResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

class NetworkQueryContactImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryContact() {

    companion object {
        private const val ENDPOINT_CONTACTS = "/contacts"
        private const val ENDPOINT_DELETE_CONTACT = "/contacts/%d"
        private const val ENDPOINT_GENERATE_TOKEN = "/contacts/tokens"

        private const val ENDPOINT_CREATE_INVITE = "/invites"
        private const val HUB_URL = "https://hub.sphinx.chat"
    }

    ///////////
    /// GET ///
    ///////////
    private val getContactsFlowNullData: Flow<LoadResponse<GetContactsResponse, ResponseError>> by lazy {
        networkRelayCall.relayGet(
            responseJsonClass = GetContactsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CONTACTS,
            relayData = null,
            useExtendedNetworkCallClient = true,
        )
    }

    override fun getContacts(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<GetContactsResponse, ResponseError>> =
        if (relayData == null) {
            getContactsFlowNullData
        } else {
            networkRelayCall.relayGet(
                responseJsonClass = GetContactsRelayResponse::class.java,
                relayEndpoint = ENDPOINT_CONTACTS,
                relayData = relayData,
                useExtendedNetworkCallClient = true,
            )
        }

    ///////////
    /// PUT ///
    ///////////
    override fun updateContact(
        contactId: ContactId,
        putContactDto: PutContactDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = ContactRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CONTACTS + "/${contactId.value}",
            requestBodyJsonClass = PutContactDto::class.java,
            requestBody = putContactDto,
            relayData = relayData,
        )

    ////////////
    /// POST ///
    ////////////
//    app.post('/contacts/:id/keys', contacts.exchangeKeys)
//    app.post('/contacts', contacts.createContact)

    override fun generateToken(
        relayUrl: RelayUrl,
        token: AuthorizationToken,
        password: String?,
        pubkey: String?
    ): Flow<LoadResponse<GenerateTokenResponse, ResponseError>> {
        return networkRelayCall.relayUnauthenticatedPost(
            responseJsonClass = GenerateTokenRelayResponse::class.java,
            relayEndpoint = ENDPOINT_GENERATE_TOKEN,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("token", token.value),
                Pair("password", password),
                Pair("pubkey", pubkey),
            ),
            relayUrl = relayUrl
        )
    }

    override fun createContact(
        postContactDto: PostContactDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = ContactRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CONTACTS,
            requestBodyJsonClass = PostContactDto::class.java,
            requestBody = postContactDto,
            relayData = relayData
        )

    //////////////
    /// DELETE ///
    //////////////
    override suspend fun deleteContact(
        contactId: ContactId,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Response<Any, ResponseError> {

        var response: Response<Any, ResponseError> = Response.Success(true)

        networkRelayCall.relayDelete(
            DeleteContactRelayResponse::class.java,
            String.format(ENDPOINT_DELETE_CONTACT, contactId.value),
            requestBody = null
        ).collect { loadResponse ->
            if (loadResponse is Response.Error) {
                response = loadResponse
            }
        }

        return response
    }

    override fun createNewInvite(
        nickname: String,
        welcomeMessage: String,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = ContactRelayResponse::class.java,
            relayEndpoint = HUB_URL + ENDPOINT_CREATE_INVITE,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("nickname", nickname),
                Pair("welcome_message", welcomeMessage),
            ),
            relayData = relayData
        )
}
