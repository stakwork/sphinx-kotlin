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
        private const val ENDPOINT_HAS_ADMIN = "/has_admin"
    }

    ///////////
    /// GET ///
    ///////////

    override fun hasAdmin(
        url: RelayUrl
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.get(
            url = "${url.value}$ENDPOINT_HAS_ADMIN",
            responseJsonClass = HasAdminRelayResponse::class.java,
        )

}
