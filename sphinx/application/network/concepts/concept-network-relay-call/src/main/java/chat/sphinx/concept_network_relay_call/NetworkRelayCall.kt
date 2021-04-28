package chat.sphinx.concept_network_relay_call

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkRelayCall {

    enum class RequestType {
        GET, POST, PUT, DELETE
    }

    /**
     * GET
     *
     * @param [jsonAdapter] the class to serialize json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<T: Any, V: RelayResponse<T>> get(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestType: RequestType = RequestType.GET,
        requestBody: Map<String, String>? = null,
        additionalHeaders: Map<String, String>? = null,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

}