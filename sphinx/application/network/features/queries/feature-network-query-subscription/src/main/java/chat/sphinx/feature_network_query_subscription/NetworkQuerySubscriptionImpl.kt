package chat.sphinx.feature_network_query_subscription

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.retrieveRelayUrlAndJavaWebToken
import chat.sphinx.feature_network_query_subscription.model.GetSubscriptionRelayResponse
import chat.sphinx.feature_network_query_subscription.model.GetSubscriptionsRelayResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.network_relay_call.RelayCall
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class NetworkQuerySubscriptionImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
): NetworkQuerySubscription() {

    companion object {
        private const val ENDPOINT_SUBSCRIPTION = "/subscription"
        private const val ENDPOINT_SUBSCRIPTIONS = "/subscriptions"
    }

    ///////////
    /// GET ///
    ///////////
    override fun getSubscriptions(): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndJavaWebToken().let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emitAll(
                        getSubscriptions(response.value.first, response.value.second)
                    )
                }
            }
        }
    }

    override fun getSubscriptions(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetSubscriptionsRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_SUBSCRIPTIONS
        )

    override fun getSubscriptionById(
        subscriptionId: SubscriptionId
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndJavaWebToken().let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emitAll(
                        getSubscriptionById(response.value.first, response.value.second, subscriptionId)
                    )
                }
            }
        }
    }

    override fun getSubscriptionById(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        subscriptionId: SubscriptionId
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetSubscriptionRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_SUBSCRIPTION + "/${subscriptionId.value}"
        )

    override fun getSubscriptionsByContactId(
        contactId: ContactId
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndJavaWebToken().let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emitAll(
                        getSubscriptionsByContactId(response.value.first, response.value.second, contactId)
                    )
                }
            }
        }
    }

    override fun getSubscriptionsByContactId(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        contactId: ContactId
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetSubscriptionsRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_SUBSCRIPTIONS + "/contact/${contactId.value}",
        )

    ///////////
    /// PUT ///
    ///////////
//    app.put('/subscription/:id', subcriptions.editSubscription)
//    app.put('/subscription/:id/pause', subcriptions.pauseSubscription)
//    app.put('/subscription/:id/restart', subcriptions.restartSubscription)

    ////////////
    /// POST ///
    ////////////
//    app.post('/subscriptions', subcriptions.createSubscription)

    //////////////
    /// DELETE ///
    //////////////
//    app.delete('/subscription/:id', subcriptions.deleteSubscription)
}
