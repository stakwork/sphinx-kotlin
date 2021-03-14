package chat.sphinx.feature_network_query_contact

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_contact.model.GetContactsResponse
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_network_query_contact.model.GetContactsRelayResponse
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.network_relay_call.RelayCall
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class NetworkQueryContactImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
): NetworkQueryContact() {

    companion object {
        private const val ENDPOINT_CONTACTS = "/contacts"
    }

    ///////////
    /// GET ///
    ///////////
    override fun getContacts(): Flow<KotlinResponse<GetContactsResponse>> = flow {
        relayDataHandler.retrieveRelayUrl()?.let { relayUrl ->
            relayDataHandler.retrieveJavaWebToken()?.let { jwt ->
                emitAll(
                    getContacts(jwt, relayUrl)
                )
            } ?: emit(KotlinResponse.Error("Was unable to retrieve the JavaWebToken from storage"))
        } ?: emit(KotlinResponse.Error("Was unable to retrieve the RelayURL from storage"))
    }

    override fun getContacts(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<KotlinResponse<GetContactsResponse>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetContactsRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_CONTACTS
        )

    ///////////
    /// PUT ///
    ///////////
//    app.put('/contacts/:id', contacts.updateContact)

    ////////////
    /// POST ///
    ////////////
//    app.post('/contacts/tokens', contacts.generateToken)
//    app.post('/contacts/:id/keys', contacts.exchangeKeys)
//    app.post('/contacts', contacts.createContact)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/contacts/:id', contacts.deleteContact)
}
