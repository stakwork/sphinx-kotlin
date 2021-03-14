package chat.sphinx.feature_network_query_subscription

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_network_query_subscription.model.GetSubscriptionRelayResponse
import chat.sphinx.feature_network_query_subscription.model.GetSubscriptionsRelayResponse
import chat.sphinx.kotlin_response.KotlinResponse
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
    override fun getSubscriptions(): Flow<KotlinResponse<List<SubscriptionDto>>> = flow {
        relayDataHandler.retrieveRelayUrl()?.let { relayUrl ->
            relayDataHandler.retrieveJavaWebToken()?.let { jwt ->
                emitAll(
                    getSubscriptions(jwt, relayUrl)
                )
            } ?: emit(KotlinResponse.Error("Was unable to retrieve the JavaWebToken from storage"))
        } ?: emit(KotlinResponse.Error("Was unable to retrieve the RelayURL from storage"))
    }

    override fun getSubscriptions(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<KotlinResponse<List<SubscriptionDto>>> =
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
    ): Flow<KotlinResponse<SubscriptionDto>> = flow {
        relayDataHandler.retrieveRelayUrl()?.let { relayUrl ->
            relayDataHandler.retrieveJavaWebToken()?.let { jwt ->
                emitAll(
                    getSubscriptionById(jwt, relayUrl, subscriptionId)
                )
            } ?: emit(KotlinResponse.Error("Was unable to retrieve the JavaWebToken from storage"))
        } ?: emit(KotlinResponse.Error("Was unable to retrieve the RelayURL from storage"))
    }

    override fun getSubscriptionById(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        subscriptionId: SubscriptionId
    ): Flow<KotlinResponse<SubscriptionDto>> =
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
    ): Flow<KotlinResponse<List<SubscriptionDto>>> = flow {
        relayDataHandler.retrieveRelayUrl()?.let { relayUrl ->
            relayDataHandler.retrieveJavaWebToken()?.let { jwt ->
                emitAll(
                    getSubscriptionsByContactId(jwt, relayUrl, contactId)
                )
            } ?: emit(KotlinResponse.Error("Was unable to retrieve the JavaWebToken from storage"))
        } ?: emit(KotlinResponse.Error("Was unable to retrieve the RelayURL from storage"))
    }

    override fun getSubscriptionsByContactId(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        contactId: ContactId
    ): Flow<KotlinResponse<List<SubscriptionDto>>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetSubscriptionsRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_SUBSCRIPTIONS + "/contact/${contactId.value}"
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