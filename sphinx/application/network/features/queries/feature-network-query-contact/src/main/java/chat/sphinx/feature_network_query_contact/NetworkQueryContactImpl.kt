package chat.sphinx.feature_network_query_contact

import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.*
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_contact.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.contact.Blocked
import chat.sphinx.wrapper_common.contact.isTrue
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

class NetworkQueryContactImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryContact() {

    companion object {
        private const val ENDPOINT_CONTACTS = "/contacts"
        private const val ENDPOINT_LATEST_CONTACTS = "/latest_contacts"
        private const val ENDPOINT_DELETE_CONTACT = "/contacts/%d"
        private const val ENDPOINT_TRIBE_MEMBERS = "/contacts/%d"
        private const val ENDPOINT_GENERATE_TOKEN = "/contacts/tokens"
        private const val ENDPOINT_KEYS_EXCHANGE = "/contacts/%d/keys"
        private const val ENDPOINT_GENERATE_GITHUB_PAT = "/bot/git"
        private const val ENDPOINT_HAS_ADMIN = "/has_admin"
        private const val ENDPOINT_DELETE_ACCOUNT = "/test_clear"


        private const val ENDPOINT_CREATE_INVITE = "/invites"

        private const val ENDPOINT_BLOCK_CONTACT = "/%s/%d"
        private const val BLOCK_CONTACT = "block"
        private const val UN_BLOCK_CONTACT = "unblock"
    }

    ///////////
    /// GET ///
    ///////////
    private val getContactsFlowNullData: Flow<LoadResponse<GetContactsResponse, ResponseError>> by lazy {
        networkRelayCall.relayGet(
            responseJsonClass = GetContactsRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_CONTACTS?from_group=false",
            relayData = null,
            useExtendedNetworkCallClient = true,
        )
    }

    override fun getContacts(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<GetContactsResponse, ResponseError>> =
        if (relayData == null) {
            getContactsFlowNullData
        } else {
            networkRelayCall.relayGet(
                responseJsonClass = GetContactsRelayResponse::class.java,
                relayEndpoint = "$ENDPOINT_CONTACTS?from_group=false",
                relayData = relayData,
                useExtendedNetworkCallClient = true,
            )
        }

    override fun getLatestContacts(
        date: DateTime?,
        limit: Int,
        offset: Int,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<GetLatestContactsResponse, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetLatestContactsRelayResponse::class.java,
            relayEndpoint = if (date != null) {
                "$ENDPOINT_LATEST_CONTACTS" +
                        "?date=${MessagePagination.getFormatPaginationPercentEscaped().format(date?.value)}" +
                        "&offset=${offset}&limit=${limit}"
            } else {
                ENDPOINT_LATEST_CONTACTS
            },
            relayData = relayData,
            useExtendedNetworkCallClient = true,
        )

    override fun getTribeMembers(
        chatId: ChatId,
        offset: Int,
        limit: Int,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<GetTribeMembersResponse, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetTribeMembersRelayResponse::class.java,
            relayEndpoint = "${String.format(ENDPOINT_TRIBE_MEMBERS, chatId.value)}?offset=$offset&limit=$limit",
            relayData = relayData
        )

    ///////////
    /// PUT ///
    ///////////
    override fun updateContact(
        contactId: ContactId,
        putContactDto: PutContactDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = ContactRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CONTACTS + "/${contactId.value}",
            requestBodyJsonClass = PutContactDto::class.java,
            requestBody = putContactDto,
            relayData = relayData,
        )

    override fun toggleBlockedContact(
        contactId: ContactId,
        blocked: Blocked,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        toggleBlockedContactImpl(
            endpoint = String.format(ENDPOINT_BLOCK_CONTACT, (if (blocked.isTrue()) UN_BLOCK_CONTACT else BLOCK_CONTACT), contactId.value),
            relayData = relayData
        )

    private fun toggleBlockedContactImpl(
        endpoint: String,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = ContactRelayResponse::class.java,
            relayEndpoint = endpoint,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    ////////////
    /// POST ///
    ////////////
//    app.post('/contacts/:id/keys', contacts.exchangeKeys)
//    app.post('/contacts', contacts.createContact)

    override fun generateToken(
        password: String?,
        publicKey: String?,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<GenerateTokenResponse, ResponseError>> {
        return networkRelayCall.relayPost(
            responseJsonClass = GenerateTokenRelayResponse::class.java,
            relayEndpoint = ENDPOINT_GENERATE_TOKEN,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("password", password),
                Pair("pubkey", publicKey),
            ),
            relayData = relayData
        )
    }

    override fun generateToken(
        relayUrl: RelayUrl,
        token: AuthorizationToken,
        password: String?,
        publicKey: String?
    ): Flow<LoadResponse<GenerateTokenResponse, ResponseError>> {
        return networkRelayCall.relayUnauthenticatedPost(
                responseJsonClass = GenerateTokenRelayResponse::class.java,
                relayEndpoint = ENDPOINT_GENERATE_TOKEN,
                requestBodyJsonClass = Map::class.java,
                requestBody = mapOf(
                    Pair("token", token.value),
                    Pair("password", password),
                    Pair("pubkey", publicKey),
                ),
                relayUrl = relayUrl
            )
    }

    override fun createContact(
        postContactDto: PostContactDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = ContactRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CONTACTS,
            requestBodyJsonClass = PostContactDto::class.java,
            requestBody = postContactDto,
            relayData = relayData
        )

    override fun generateGithubPAT(
        patDto: GithubPATDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any, ResponseError>> {
        return networkRelayCall.relayPost(
            responseJsonClass = GenerateGithubPATRelayResponse::class.java,
            relayEndpoint = ENDPOINT_GENERATE_GITHUB_PAT,
            requestBodyJsonClass = GithubPATDto::class.java,
            requestBody = patDto,
            relayData = relayData
        )
    }

    //////////////
    /// DELETE ///
    //////////////
    override suspend fun deleteContact(
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
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
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = CreateInviteRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CREATE_INVITE,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(
                Pair("nickname", nickname),
                Pair("welcome_message", welcomeMessage),
            ),
            relayData = relayData
        )

    override fun exchangeKeys(
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = ContactRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_KEYS_EXCHANGE, contactId.value),
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    override fun hasAdmin(
        url: RelayUrl
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.get(
            url = "${url.value}$ENDPOINT_HAS_ADMIN",
            responseJsonClass = HasAdminRelayResponse::class.java,
        )

    override fun deleteAccount(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = DeleteAccountRelayResponse::class.java,
            relayEndpoint = ENDPOINT_DELETE_ACCOUNT,
            relayData = relayData
        )

}
