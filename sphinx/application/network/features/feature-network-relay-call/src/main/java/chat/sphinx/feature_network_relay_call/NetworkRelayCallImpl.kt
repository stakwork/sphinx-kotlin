package chat.sphinx.feature_network_relay_call

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.concept_network_relay_call.RelayResponse
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.retrieveRelayUrlAndAuthorizationToken
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.message
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.internal.EMPTY_REQUEST
import java.io.IOException


@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
private inline fun NetworkRelayCallImpl.buildRelayRequest(
    relayEndpoint: String,
    relayData: Pair<AuthorizationToken, RelayUrl>,
    additionalHeaders: Map<String, String>?
): Request.Builder {
    val builder = Request.Builder()

    builder.addHeader(AuthorizationToken.AUTHORIZATION_HEADER, relayData.first.value)
    builder.url(relayData.second.value + relayEndpoint)

    additionalHeaders?.let { headers ->
        for (header in headers) {
            builder.addHeader(header.key, header.value)
        }
    }

    return builder
}

@Suppress("NOTHING_TO_INLINE")
private inline fun NetworkRelayCallImpl.handleException(
    LOG: SphinxLogger,
    callMethod: String,
    relayEndpoint: String,
    e: Exception
): Response.Error<ResponseError> {
    val msg = "$callMethod Request failure for endpoint: $relayEndpoint"
    LOG.e(NetworkRelayCallImpl.TAG, msg, e)
    return Response.Error(ResponseError(msg, e))
}

@Throws(Exception::class)
private suspend inline fun RelayDataHandler.retrieveRelayData(): Pair<AuthorizationToken, RelayUrl> {
    val response = retrieveRelayUrlAndAuthorizationToken()

    @Exhaustive
    when(response) {
        is Response.Error -> {
            throw Exception(response.message)
        }
        is Response.Success -> {
            return response.value
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
@Throws(AssertionError::class, NullPointerException::class)
private inline fun<RequestBody: Any> Moshi.requestBodyToJson(
    requestBodyJsonAdapter: Class<RequestBody>,
    requestBody: RequestBody
): String =
    adapter(requestBodyJsonAdapter)
        .toJson(requestBody)
        ?: throw NullPointerException(
            "Failed to convert RequestBody ${requestBodyJsonAdapter.simpleName} to Json"
        )

@Suppress("BlockingMethodInNonBlockingContext")
class NetworkRelayCallImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler,
    private val LOG: SphinxLogger,
): NetworkRelayCall() {

    companion object {
        const val TAG = "NetworkRelayCallImpl"
    }

    ///////////////////
    /// NetworkCall ///
    ///////////////////
    override fun <T: Any> get(
        jsonAdapter: Class<T>,
        url: String,
        headers: Map<String, String>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

    }.flowOn(dispatchers.io)

    override fun <T: Any, V: Any> put(
        jsonAdapter: Class<T>,
        url: String,
        requestBodyJsonAdapter: Class<V>,
        requestBody: V,
        mediaType: String?,
        headers: Map<String, String>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

    }.flowOn(dispatchers.io)

    override fun <T: Any, V: Any> post(
        jsonAdapter: Class<T>,
        url: String,
        requestBodyJsonAdapter: Class<V>,
        requestBody: V,
        mediaType: String?,
        headers: Map<String, String>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

    }.flowOn(dispatchers.io)

    override fun <T: Any, V: Any> delete(
        jsonAdapter: Class<T>,
        url: String,
        requestBodyJsonAdapter: Class<V>?,
        requestBody: V?,
        mediaType: String?,
        headers: Map<String, String>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

    }.flowOn(dispatchers.io)

    ////////////////////////
    /// NetworkRelayCall ///
    ////////////////////////
    override fun <T: Any, V: RelayResponse<T>> get(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRelayRequest(
                relayEndpoint,
                relayData ?: relayDataHandler.retrieveRelayData(),
                additionalHeaders
            )

            val response = call(jsonAdapter, requestBuilder.build())

            emit(Response.Success(response))

        } catch (e: Exception) {
            emit(handleException(LOG, "GET", relayEndpoint, e))
        }

    }.flowOn(dispatchers.io)

    override fun <T: Any, RequestBody: Any, V: RelayResponse<T>> put(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestBodyJsonAdapter: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String?,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRelayRequest(
                relayEndpoint,
                relayData ?: relayDataHandler.retrieveRelayData(),
                additionalHeaders
            )

            val requestBodyJson: String = moshi
                .requestBodyToJson(requestBodyJsonAdapter, requestBody)

            val reqBody = requestBodyJson.toRequestBody(mediaType?.toMediaType())

            val response = call(jsonAdapter, requestBuilder.put(reqBody).build())

            emit(Response.Success(response))

        } catch (e: Exception) {
            emit(handleException(LOG, "PUT", relayEndpoint, e))
        }

    }.flowOn(dispatchers.io)

    override fun <T: Any, RequestBody: Any, V: RelayResponse<T>> post(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestBodyJsonAdapter: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String?,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRelayRequest(
                relayEndpoint,
                relayData ?: relayDataHandler.retrieveRelayData(),
                additionalHeaders
            )

            val requestBodyJson: String = moshi
                .requestBodyToJson(requestBodyJsonAdapter, requestBody)

            val reqBody = requestBodyJson.toRequestBody(mediaType?.toMediaType())

            val response = call(jsonAdapter, requestBuilder.post(reqBody).build())

            emit(Response.Success(response))

        } catch (e: Exception) {
            emit(handleException(LOG, "POST", relayEndpoint, e))
        }

    }.flowOn(dispatchers.io)

    override fun <T: Any, RequestBody: Any, V: RelayResponse<T>> delete(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        requestBodyJsonAdapter: Class<RequestBody>?,
        requestBody: RequestBody?,
        mediaType: String?,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRelayRequest(
                relayEndpoint,
                relayData ?: relayDataHandler.retrieveRelayData(),
                additionalHeaders
            )

            val requestBodyJson: String? =
                if (requestBody == null || requestBodyJsonAdapter == null) {
                    null
                } else {
                    moshi.requestBodyToJson(requestBodyJsonAdapter, requestBody)
                }

            val reqBody = requestBodyJson?.toRequestBody(mediaType?.toMediaType())

            val response = call(jsonAdapter, requestBuilder.delete(reqBody ?: EMPTY_REQUEST).build())

            emit(Response.Success(response))

        } catch (e: Exception) {
            emit(handleException(LOG, "DELETE", relayEndpoint, e))
        }

    }.flowOn(dispatchers.io)

    @Throws(NullPointerException::class, IOException::class)
    private suspend fun<T: Any, V: RelayResponse<T>> call(jsonAdapter: Class<V>, request: Request): T {
        val networkResponse = networkClient.getClient()
            .newCall(request)
            .execute()

        if (!networkResponse.isSuccessful) {
            throw IOException(networkResponse.toString())
        }

        val body = networkResponse.body ?: throw NullPointerException(
            """
                NetworkResponse.body returned null
                NetworkResponse: $networkResponse
            """.trimIndent()
        )

        val relayResponse: V = moshi
            .adapter(jsonAdapter)
            .fromJson(body.source())
            ?: throw IOException(
                """
                    Failed to convert Json to ${jsonAdapter.simpleName}
                    NetworkResponse: $networkResponse
                """.trimIndent()
            )

        if (relayResponse.success) {
            return relayResponse.response ?: throw NullPointerException(
                """
                    RelayResponse.success: true
                    RelayResponse.response: >>> null <<<
                    RelayResponse.error: ${relayResponse.error}
                    NetworkResponse: $networkResponse
                """.trimIndent()
            )
        } else {
            throw Exception(
                """
                    RelayResponse.success: false
                    RelayResponse.error: ${relayResponse.error}
                    NetworkResponse: $networkResponse
                """.trimIndent()
            )
        }
    }
}
