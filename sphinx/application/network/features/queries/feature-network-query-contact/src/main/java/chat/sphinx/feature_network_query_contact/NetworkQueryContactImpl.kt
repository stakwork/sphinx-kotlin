package chat.sphinx.feature_network_query_contact

import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.concept_network_query_contact.model.PostContactDto
import chat.sphinx.concept_network_query_contact.model.PutContactDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_contact.model.CreateContactRelayResponse
import chat.sphinx.feature_network_query_contact.model.ContactRelayResponse
import chat.sphinx.feature_network_query_contact.model.DeleteContactRelayResponse
import chat.sphinx.feature_network_query_contact.model.GetContactsRelayResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_common.contact.ContactId
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
        private const val ENDPOINT_CREATE_CONTACT = "/contacts"
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
//    app.post('/contacts/tokens', contacts.generateToken)
//    app.post('/contacts/:id/keys', contacts.exchangeKeys)
//    app.post('/contacts', contacts.createContact)

    override fun createContact(
        contact: PostContactDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ContactDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = CreateContactRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CREATE_CONTACT,
            requestBodyJsonClass = PostContactDto::class.java,
            requestBody = contact,
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
}
