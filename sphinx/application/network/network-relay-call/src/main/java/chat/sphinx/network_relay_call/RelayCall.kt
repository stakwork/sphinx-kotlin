package chat.sphinx.network_relay_call

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.wrapper_relay.JavaWebToken
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.IOException

sealed class RelayCall {

    object Get: RelayCall() {

        @Suppress("BlockingMethodInNonBlockingContext")
        fun<T: Any, V: RelayResponse<T>> execute(
            dispatchers: CoroutineDispatchers,
            jwt: JavaWebToken,
            moshi: Moshi,
            adapterClass: Class<V>,
            networkClient: NetworkClient,
            url: String,
            additionalHeaders: Map<String, String>? = null
        ): Flow<KotlinResponse<T>> = flow {

            emit(KotlinResponse.Loading)

            val request: Request = Request.Builder().let { builder ->
                builder.addHeader("X-User-Token", jwt.value)

                additionalHeaders?.let { headers ->
                    for (header in headers) {
                        builder.addHeader(header.key, header.value)
                    }
                }

                builder.url(url)
                builder.build()
            }

            try {
                val networkResponse = networkClient
                    .getClient()
                    .newCall(request)
                    .execute()

                if (!networkResponse.isSuccessful) {
                    throw IOException(networkResponse.toString())
                }

                val nnBody = networkResponse.body ?: throw NullPointerException(
                    "ResponseBody for GET request url='$url' returned null"
                )

                val relayResponse: V = moshi
                    .adapter(adapterClass)
                    .fromJson(nnBody.source())
                    ?: throw IOException(
                        "Failed to convert Json to ${adapterClass.simpleName}"
                    )

                if (relayResponse.success) {

                    emit(
                        KotlinResponse.Success(
                            relayResponse.response ?: throw NullPointerException(
                                "RelayResponse.response was null"
                            )
                        )
                    )

                } else {

                    emit(
                        KotlinResponse.Error(
                            relayResponse.error ?: "Query failed for ${adapterClass.simpleName}"
                        )
                    )

                }

            } catch (e: Exception) {

                emit(KotlinResponse.Error("", e))

            }

        }.flowOn(dispatchers.io)
    }

    object Put: RelayCall()

    object Post: RelayCall()

    object Delete: RelayCall()

}
