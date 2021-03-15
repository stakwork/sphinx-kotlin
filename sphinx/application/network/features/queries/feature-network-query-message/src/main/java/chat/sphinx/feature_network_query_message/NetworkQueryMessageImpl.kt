package chat.sphinx.feature_network_query_message

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_message.model.GetMessagesResponse
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_network_query_message.model.GetMessagesRelayResponse
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.network_relay_call.RelayCall
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class NetworkQueryMessageImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
): NetworkQueryMessage() {

    companion object {
        private const val ENDPOINT_MSGS = "/msgs"
        private const val ENDPOINT_MESSAGE = "/message"
        private const val ENDPOINT_MESSAGES = "/messages"
    }

    ///////////
    /// GET ///
    ///////////
    override fun getMessages(
        messagePagination: MessagePagination?
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrl()?.let { relayUrl ->
            relayDataHandler.retrieveJavaWebToken()?.let { jwt ->
                emitAll(
                    getMessages(jwt, relayUrl, messagePagination)
                )
            } ?: emit(
                KotlinResponse.Error(
                    ResponseError("Was unable to retrieve the JavaWebToken from storage")
                )
            )
        } ?: emit(
            KotlinResponse.Error(
                ResponseError("Was unable to retrieve the RelayURL from storage")
            )
        )
    }

    override fun getMessages(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        messagePagination: MessagePagination?,
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetMessagesRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_MSGS + (messagePagination?.value ?: "")
        )

    ///////////
    /// PUT ///
    ///////////

    ////////////
    /// POST ///
    ////////////
//    app.post('/messages', messages.sendMessage)
//    app.post('/messages/:chat_id/read', messages.readMessages)
//    app.post('/messages/clear', messages.clearMessages)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/message/:id', messages.deleteMessage)
}
