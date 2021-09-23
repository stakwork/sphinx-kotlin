package chat.sphinx.feature_network_relay_call

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_call.buildRequest
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_client.NetworkClientClearedListener
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
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import java.io.IOException
import java.util.concurrent.TimeUnit

@Suppress("NOTHING_TO_INLINE")
private inline fun NetworkRelayCallImpl.mapRelayHeaders(
    relayData: Pair<AuthorizationToken, RelayUrl>,
    additionalHeaders: Map<String, String>?
): Map<String, String> {
    val map: MutableMap<String, String> = mutableMapOf(
        Pair(AuthorizationToken.AUTHORIZATION_HEADER, relayData.first.value)
    )

    additionalHeaders?.let {
        map.putAll(it)
    }

    return map
}

@Suppress("NOTHING_TO_INLINE")
private inline fun NetworkRelayCallImpl.handleException(
    LOG: SphinxLogger,
    callMethod: String,
    url: String,
    e: Exception
): Response.Error<ResponseError> {
    val msg = "$callMethod Request failure for: $url"
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
private suspend inline fun<RequestBody: Any> Moshi.requestBodyToJson(
    dispatchers: CoroutineDispatchers,
    requestBodyJsonAdapter: Class<RequestBody>,
    requestBody: RequestBody
): String =
    withContext(dispatchers.default) {
        adapter(requestBodyJsonAdapter).toJson(requestBody)
    } ?: throw NullPointerException(
        "Failed to convert RequestBody ${requestBodyJsonAdapter.simpleName} to Json"
    )

@Suppress("BlockingMethodInNonBlockingContext")
class NetworkRelayCallImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler,
    private val LOG: SphinxLogger,
) : NetworkRelayCall(),
    NetworkClientClearedListener,
    CoroutineDispatchers by dispatchers
{

    companion object {
        const val TAG = "NetworkRelayCallImpl"

        private const val GET = "GET"
        private const val PUT = "PUT"
        private const val POST = "POST"
        private const val DELETE = "DELETE"
    }

    ///////////////////
    /// NetworkCall ///
    ///////////////////
    override fun <T: Any> get(
        url: String,
        responseJsonClass: Class<T>,
        headers: Map<String, String>?,
        useExtendedNetworkCallClient: Boolean
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRequest(url, headers)

            val response = call(responseJsonClass, requestBuilder.build(), useExtendedNetworkCallClient)

            emit(Response.Success(response))
        } catch (e: Exception) {
            emit(handleException(LOG, GET, url, e))
        }
    }
    
    override fun <T: Any> getList(
        url: String,
        responseJsonClass: Class<T>,
        headers: Map<String, String>?,
        useExtendedNetworkCallClient: Boolean,
    ): Flow<LoadResponse<List<T>, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRequest(url, headers)

            val response = callList(responseJsonClass, requestBuilder.build(), useExtendedNetworkCallClient)

            emit(Response.Success(response))
        } catch (e: Exception) {
            emit(handleException(LOG, GET, url, e))
        }
    }

    override fun <T: Any, V: Any> put(
        url: String,
        responseJsonClass: Class<T>,
        requestBodyJsonClass: Class<V>?,
        requestBody: V?,
        mediaType: String?,
        headers: Map<String, String>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRequest(url, headers)

            val requestBodyJson: String? = if (requestBody == null || requestBodyJsonClass == null) {
                null
            } else {
                moshi.requestBodyToJson(dispatchers, requestBodyJsonClass, requestBody)
            }

            val reqBody = requestBodyJson?.toRequestBody(mediaType?.toMediaType())

            val response = call(responseJsonClass, requestBuilder.put(reqBody ?: EMPTY_REQUEST).build())

            emit(Response.Success(response))
        } catch (e: Exception) {
            emit(handleException(LOG, PUT, url, e))
        }

    }

    override fun <T: Any, V: Any> post(
        url: String,
        responseJsonClass: Class<T>,
        requestBodyJsonClass: Class<V>,
        requestBody: V,
        mediaType: String?,
        headers: Map<String, String>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRequest(url, headers)

            val requestBodyJson: String = moshi
                .requestBodyToJson(dispatchers, requestBodyJsonClass, requestBody)

            val reqBody = requestBodyJson.toRequestBody(mediaType?.toMediaType())

            val response = call(responseJsonClass, requestBuilder.post(reqBody).build())

            emit(Response.Success(response))
        } catch (e: Exception) {
            emit(handleException(LOG, POST, url, e))
        }

    }

    override fun <T: Any, V: Any> delete(
        url: String,
        responseJsonClass: Class<T>,
        requestBodyJsonClass: Class<V>?,
        requestBody: V?,
        mediaType: String?,
        headers: Map<String, String>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRequest(url, headers)

            val requestBodyJson: String? =
                if (requestBody == null || requestBodyJsonClass == null) {
                    null
                } else {
                    moshi.requestBodyToJson(dispatchers, requestBodyJsonClass, requestBody)
                }

            val reqBody: RequestBody? = requestBodyJson?.toRequestBody(mediaType?.toMediaType())

            val response = call(responseJsonClass, requestBuilder.delete(reqBody ?: EMPTY_REQUEST).build())

            emit(Response.Success(response))
        } catch (e: Exception) {
            emit(handleException(LOG, DELETE, url, e))
        }

    }

    @Volatile
    private var extendedNetworkCallClient: OkHttpClient? = null
    private val extendedClientLock = Mutex()

    override fun networkClientCleared() {
        extendedNetworkCallClient = null
    }

    init {
        networkClient.addListener(this)
    }

    @Throws(NullPointerException::class, IOException::class)
    override suspend fun <T: Any> call(
        responseJsonClass: Class<T>,
        request: Request,
        useExtendedNetworkCallClient: Boolean
    ): T {

        // TODO: Make less horrible. Needed for the `/contacts` endpoint for users who
        //  have a large number of contacts as Relay needs more time than the default
        //  client's settings. Replace once the `aa/contacts` endpoint gets pagination.
        val client = if (useExtendedNetworkCallClient) {
            extendedClientLock.withLock {
                extendedNetworkCallClient ?: networkClient.getClient().newBuilder()
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .build()
                    .also { extendedNetworkCallClient = it }
            }
        } else {
            networkClient.getClient()
        }

        val networkResponse = withContext(io) {
            client.newCall(request).execute()
        }

        if (!networkResponse.isSuccessful) {
            networkResponse.body?.close()
            throw IOException(networkResponse.toString())
        }

        val body = networkResponse.body ?: throw NullPointerException(
            """
                NetworkResponse.body returned null
                NetworkResponse: $networkResponse
            """.trimIndent()
        )

        return withContext(default) {
            moshi.adapter(responseJsonClass).fromJson(body.source())
        } ?: throw IOException(
            """
                Failed to convert Json to ${responseJsonClass.simpleName}
                NetworkResponse: $networkResponse
            """.trimIndent()
        )
    }

    @Throws(NullPointerException::class, IOException::class)
    override suspend fun <T: Any> callList(
        responseJsonClass: Class<T>,
        request: Request,
        useExtendedNetworkCallClient: Boolean
    ): List<T> {
        // TODO: Make less horrible. Needed for the `/contacts` endpoint for users who
        //  have a large number of contacts as Relay needs more time than the default
        //  client's settings. Replace once the `aa/contacts` endpoint gets pagination.
        val client = if (useExtendedNetworkCallClient) {
            extendedClientLock.withLock {
                extendedNetworkCallClient ?: networkClient.getClient().newBuilder()
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .build()
                    .also { extendedNetworkCallClient = it }
            }
        } else {
            networkClient.getClient()
        }

        val networkResponse = withContext(io) {
            client.newCall(request).execute()
        }

        if (!networkResponse.isSuccessful) {
            networkResponse.body?.close()
            throw IOException(networkResponse.toString())
        }

        val body = networkResponse.body ?: throw NullPointerException(
            """
                NetworkResponse.body returned null
                NetworkResponse: $networkResponse
            """.trimIndent()
        )

        val listMyData = Types.newParameterizedType(List::class.java, responseJsonClass)

        return withContext(default) {
            moshi.adapter<List<T>>(listMyData).fromJson(body.source())
        } ?: throw IOException(
            """
                Failed to convert Json to ${responseJsonClass.simpleName}
                NetworkResponse: $networkResponse
            """.trimIndent()
        )
    }

    ////////////////////////
    /// NetworkRelayCall ///
    ////////////////////////
    override fun <T: Any, V: RelayResponse<T>> relayGet(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?,
        useExtendedNetworkCallClient: Boolean,
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        val responseFlow: Flow<LoadResponse<V, ResponseError>>? = try {
            val nnRelayData: Pair<AuthorizationToken, RelayUrl> = relayData
                ?: relayDataHandler.retrieveRelayData()

            get(
                nnRelayData.second.value + relayEndpoint,
                responseJsonClass,
                mapRelayHeaders(nnRelayData, additionalHeaders),
                useExtendedNetworkCallClient
            )
        } catch (e: Exception) {
            emit(handleException(LOG, GET, relayEndpoint, e))
            null
        }

        responseFlow?.let {
            emitAll(validateRelayResponse(it, GET, relayEndpoint))
        }

    }

    override fun <T: Any, RequestBody: Any, V: RelayResponse<T>> relayPut(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>?,
        requestBody: RequestBody?,
        mediaType: String?,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        val responseFlow: Flow<LoadResponse<V, ResponseError>>? = try {
            val nnRelayData: Pair<AuthorizationToken, RelayUrl> = relayData
                ?: relayDataHandler.retrieveRelayData()

            put(
                nnRelayData.second.value + relayEndpoint,
                responseJsonClass,
                requestBodyJsonClass,
                requestBody,
                mediaType,
                mapRelayHeaders(nnRelayData, additionalHeaders)
            )
        } catch (e: Exception) {
            emit(handleException(LOG, PUT, relayEndpoint, e))
            null
        }

        responseFlow?.let {
            emitAll(validateRelayResponse(it, PUT, relayEndpoint))
        }

    }

    // TODO: Remove and replace all uses with post (DO NOT USE THIS METHOD FOR NEW CODE)
    override fun <T: Any, RequestBody: Any, V: RelayResponse<T>> relayUnauthenticatedPost(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String?,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        val responseFlow: Flow<LoadResponse<V, ResponseError>>? = try {
            post(
                relayUrl.value + relayEndpoint,
                responseJsonClass,
                requestBodyJsonClass,
                requestBody,
                mediaType
            )
        } catch (e: Exception) {
            emit(handleException(LOG, POST, relayEndpoint, e))
            null
        }

        responseFlow?.let {
            emitAll(validateRelayResponse(it, POST, relayEndpoint))
        }

    }

    override fun <T: Any, RequestBody: Any, V: RelayResponse<T>> relayPost(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>,
        requestBody: RequestBody,
        mediaType: String?,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        val responseFlow: Flow<LoadResponse<V, ResponseError>>? = try {
            val nnRelayData: Pair<AuthorizationToken, RelayUrl> = relayData
                ?: relayDataHandler.retrieveRelayData()

            post(
                nnRelayData.second.value + relayEndpoint,
                responseJsonClass,
                requestBodyJsonClass,
                requestBody,
                mediaType,
                mapRelayHeaders(nnRelayData, additionalHeaders)
            )
        } catch (e: Exception) {
            emit(handleException(LOG, POST, relayEndpoint, e))
            null
        }

        responseFlow?.let {
            emitAll(validateRelayResponse(it, POST, relayEndpoint))
        }

    }

    override fun <T: Any, RequestBody: Any, V: RelayResponse<T>> relayDelete(
        responseJsonClass: Class<V>,
        relayEndpoint: String,
        requestBodyJsonClass: Class<RequestBody>?,
        requestBody: RequestBody?,
        mediaType: String?,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        val responseFlow: Flow<LoadResponse<V, ResponseError>>? = try {
            val nnRelayData: Pair<AuthorizationToken, RelayUrl> = relayData
                ?: relayDataHandler.retrieveRelayData()

            delete(
                nnRelayData.second.value + relayEndpoint,
                responseJsonClass,
                requestBodyJsonClass,
                requestBody,
                mediaType,
                mapRelayHeaders(nnRelayData, additionalHeaders)
            )
        } catch (e: Exception) {
            emit(handleException(LOG, DELETE, relayEndpoint, e))
            null
        }

        responseFlow?.let {
            emitAll(validateRelayResponse(it, DELETE, relayEndpoint))
        }

    }

    @Throws(NullPointerException::class, AssertionError::class)
    private fun <T: Any, V: RelayResponse<T>> validateRelayResponse(
        flow: Flow<LoadResponse<V, ResponseError>>,
        callMethod: String,
        endpoint: String,
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        flow.collect { loadResponse ->

            @Exhaustive
            when (loadResponse) {
                is LoadResponse.Loading -> {
                    emit(loadResponse)
                }
                is Response.Error -> {
                    emit(loadResponse)
                }
                is Response.Success -> {

                    if (loadResponse.value.success) {

                        loadResponse.value.response?.let { nnResponse ->

                            emit(Response.Success(nnResponse))

                        } ?: let {

                            val msg = """
                                RelayResponse.success: true
                                RelayResponse.response: >>> null <<<
                                RelayResponse.error: ${loadResponse.value.error}
                            """.trimIndent()

                            emit(handleException(LOG, callMethod, endpoint, NullPointerException(msg)))

                        }

                    } else {

                        val msg = """
                            RelayResponse.success: false
                            RelayResponse.error: ${loadResponse.value.error}
                        """.trimIndent()

                        emit(handleException(LOG, callMethod, endpoint, Exception(msg)))

                    }
                }

            }

        }

    }

}
