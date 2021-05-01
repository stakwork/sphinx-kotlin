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
     * @param [jsonAdapter] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<T: Any, V: RelayResponse<T>> get(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        additionalHeaders: Map<String, String>? = null,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * PUT
     *
     * @param [jsonAdapter] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [requestBodyJsonAdapter] the class to serialize the request body to json
     * @param [requestBody] the request body to be converted to json
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > put(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestBodyJsonAdapter: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String? = "application/json",
        additionalHeaders: Map<String, String>? = null,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * POST
     *
     * @param [jsonAdapter] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [requestBodyJsonAdapter] the class to serialize the request body to json
     * @param [requestBody] the request body to be converted to json
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > post(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestBodyJsonAdapter: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String? = "application/json",
        additionalHeaders: Map<String, String>? = null,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * DELETE
     *
     * @param [jsonAdapter] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [requestBodyJsonAdapter] OPTIONAL: the class to serialize the request body to json
     * @param [requestBody] OPTIONAL: the request body to be converted to json
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > delete(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestBodyJsonAdapter: Class<RequestBody>? = null,
        requestBody: RequestBody? = null,
        mediaType: String? = null,
        additionalHeaders: Map<String, String>? = null,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

}