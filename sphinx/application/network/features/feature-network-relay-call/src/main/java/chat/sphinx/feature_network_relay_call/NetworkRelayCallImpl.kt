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
import java.io.IOException

@Suppress("NOTHING_TO_INLINE")
private inline fun NetworkRelayCallImpl.buildRelayRequest(
    relayEndpoint: String,
    relayData: Pair<AuthorizationToken, RelayUrl>,
    additionalHeaders: Map<String, String>?
): Request.Builder {
    val builder = Request.Builder()

    builder.addHeader(NetworkRelayCallImpl.AUTHORIZATION_HEADER, relayData.first.value)
    builder.url(relayData.second.value + relayEndpoint)

    additionalHeaders?.let { headers ->
        for (header in headers) {
            builder.addHeader(header.key, header.value)
        }
    }

    return builder
}

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
        const val AUTHORIZATION_HEADER = "X-User-Token"
    }

    override fun <T : Any, V : RelayResponse<T>> get(
        jsonAdapter: Class<V>,
        relayEndpoint: String,
        additionalHeaders: Map<String, String>?,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<T, ResponseError>> = flow {

        emit(LoadResponse.Loading)

        try {
            val requestBuilder = buildRelayRequest(
                relayEndpoint,
                relayData ?: retrieveRelayData(),
                additionalHeaders
            )

            val networkResponse = networkClient.getClient()
                .newCall(requestBuilder.build())
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

                emit(
                    Response.Success(
                        relayResponse.response ?: throw NullPointerException(
                            """
                                RelayResponse.success: true
                                RelayResponse.response: >>> null <<<
                                RelayResponse.error: ${relayResponse.error}
                                NetworkResponse: $networkResponse
                            """.trimIndent()
                        )
                    )
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

        } catch (e: Exception) {
            val msg = "GET request failure for endpoint: $relayEndpoint"
            LOG.e(TAG, msg, e)
            emit(Response.Error(
                ResponseError(msg, e)
            ))
        }

    }.flowOn(dispatchers.io)

    @Throws(Exception::class)
    private suspend fun retrieveRelayData(): Pair<AuthorizationToken, RelayUrl> {
        val response = relayDataHandler.retrieveRelayUrlAndAuthorizationToken()

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
}
