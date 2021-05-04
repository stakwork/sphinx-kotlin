package chat.sphinx.concept_network_relay_call

import chat.sphinx.concept_network_call.NetworkCall
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

/**
 * Methods for GET/PUT/POST/DELETE that are specific to interacting with Relay.
 *
 * It automatically adds Headers for the Account Owner's [AuthorizationToken],
 * and handles success/errors depending on [RelayResponse.success].
 * */
abstract class NetworkRelayCall: NetworkCall() {

    /**
     * GET
     *
     * @param [jsonAdapter] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<T: Any, V: RelayResponse<T>> relayGet(
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
     * @param [mediaType] the media type for the request body, defaults to "application/json"
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > relayPut(
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
     * @param [mediaType] the media type for the request body, defaults to "application/json"
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > relayPost(
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
     * @param [mediaType] OPTIONAL: the media type for the request body
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun<
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > relayDelete(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestBodyJsonAdapter: Class<RequestBody>? = null,
        requestBody: RequestBody? = null,
        mediaType: String? = null,
        additionalHeaders: Map<String, String>? = null,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

}
