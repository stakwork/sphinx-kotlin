package chat.sphinx.concept_network_call

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.flow.Flow

/**
 * Methods for GET/PUT/POST/DELETE for general, non-Relay specific network queries.
 * */
abstract class NetworkCall {

    /**
    * GET
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [url] the url
     * @param [headers] any headers that need to be added to the request
    * */
    abstract fun <T: Any> get(
        url: String,
        responseJsonClass: Class<T>,
        headers: Map<String, String>? = null,
        useExtendedNetworkCallClient: Boolean = false,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * PUT
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [url] the url
     * @param [requestBodyJsonClass] the class to serialize the request body to json
     * @param [requestBody] the request body to be converted to json
     * @param [mediaType] the media type for the request body, defaults to "application/json"
     * @param [headers] any headers that need to be added to the request
     * */
    abstract fun <T: Any, RequestBody: Any> put(
        url: String,
        responseJsonClass: Class<T>,
        requestBodyJsonClass: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String? = "application/json",
        headers: Map<String, String>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * POST
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [url] the url
     * @param [requestBodyJsonClass] the class to serialize the request body to json
     * @param [requestBody] the request body to be converted to json
     * @param [mediaType] the media type for the request body, defaults to "application/json"
     * @param [headers] any headers that need to be added to the request
     * */
    abstract fun <T: Any, RequestBody: Any> post(
        url: String,
        responseJsonClass: Class<T>,
        requestBodyJsonClass: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String? = "application/json",
        headers: Map<String, String>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

    /**
     * DELETE
     *
     * @param [responseJsonClass] the class to serialize the response json into
     * @param [url] the url
     * @param [requestBodyJsonClass] OPTIONAL: the class to serialize the request body to json
     * @param [requestBody] OPTIONAL: the request body to be converted to json
     * @param [mediaType] OPTIONAL: the media type for the request body
     * @param [headers] any headers that need to be added to the request
     * */
    abstract fun <T: Any, RequestBody: Any> delete(
        url: String,
        responseJsonClass: Class<T>,
        requestBodyJsonClass: Class<RequestBody>? = null,
        requestBody: RequestBody? = null,
        mediaType: String? = null,
        headers: Map<String, String>? = null,
    ): Flow<LoadResponse<T, ResponseError>>

}
