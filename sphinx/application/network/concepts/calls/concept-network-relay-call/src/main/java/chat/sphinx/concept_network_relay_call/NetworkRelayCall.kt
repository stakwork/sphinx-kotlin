package chat.sphinx.concept_network_relay_call

import chat.sphinx.concept_network_call.NetworkCall
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

/**
 * Methods for GET/PUT/POST/DELETE that are specific to interacting with Relay.
 *
 * It automatically:
 *  - Retrieves from persistent storage the [RelayUrl] and [AuthorizationToken]
 *  for all queries if `null` is passed for that argument.
 *  - Adds the Authorization RequestHeader.
 *  - Handles [RelayResponse.success] when `false` by returning a [ResponseError]
 *  instead.
 *  - Json serialization/deserialization
 * */
abstract class NetworkRelayCall: NetworkCall() {

    /**
     * GET
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun <
            T: Any,
            V: RelayResponse<T>
            > relayGet(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        additionalHeaders: Map<String, String>? = null,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null,
        useExtendedNetworkCallClient: Boolean = false,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * GET
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun <
            T: Any,
            V: RelayListResponse<T>
            > relayGetList(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        additionalHeaders: Map<String, String>? = null,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null,
        useExtendedNetworkCallClient: Boolean = false,
    ): Flow<LoadResponse<List<T>, ResponseError>>

    /**
     * GET
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayUrl] unauthenticated relay URL
     * */
    abstract fun <
            T: Any,
            V: RelayResponse<T>
            > relayUnauthenticatedGet(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * PUT
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [requestBodyJsonClass] the class to serialize the request body to json
     * @param [requestBody] the request body to be converted to json
     * @param [mediaType] the media type for the request body, defaults to "application/json"
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun <
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > relayPut(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>? = null,
        requestBody: RequestBody? = null,
        mediaType: String? = "application/json",
        additionalHeaders: Map<String, String>? = null,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

    // TODO: Remove and replace all uses with post (DO NOT USE THIS METHOD FOR NEW CODE)
    /**
     * POST
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [requestBodyJsonClass] the class to serialize the request body to json
     * @param [requestBody] the request body to be converted to json
     * @param [mediaType] the media type for the request body, defaults to "application/json"
     * @param [relayUrl] unauthenticated relay URL
     * */
    @Deprecated(message = "do not use")
    abstract fun <
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > relayUnauthenticatedPost(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String? = "application/json",
        relayUrl: RelayUrl,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * POST
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [requestBodyJsonClass] the class to serialize the request body to json
     * @param [requestBody] the request body to be converted to json
     * @param [mediaType] the media type for the request body, defaults to "application/json"
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun <
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > relayPost(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String? = "application/json",
        additionalHeaders: Map<String, String>? = null,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * DELETE
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [relayEndpoint] the endpoint to append to the [RelayUrl], ex: /contacts
     * @param [requestBodyJsonClass] OPTIONAL: the class to serialize the request body to json
     * @param [requestBody] OPTIONAL: the request body to be converted to json
     * @param [mediaType] OPTIONAL: the media type for the request body
     * @param [additionalHeaders] any additional headers to add to the call
     * @param [relayData] if not `null`, will override the auto-fetching of persisted user data
     * */
    abstract fun <
            T: Any,
            RequestBody: Any,
            V: RelayResponse<T>
            > relayDelete(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>? = null,
        requestBody: RequestBody? = null,
        mediaType: String? = null,
        additionalHeaders: Map<String, String>? = null,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

}
