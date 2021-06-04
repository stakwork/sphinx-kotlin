package chat.sphinx.feature_network_query_subscription

import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_subscription.model.GetSubscriptionRelayResponse
import chat.sphinx.feature_network_query_subscription.model.GetSubscriptionsRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQuerySubscriptionImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQuerySubscription() {

    companion object {
        private const val ENDPOINT_SUBSCRIPTION = "/subscription"
        private const val ENDPOINT_SUBSCRIPTIONS = "/subscriptions"
    }

    ///////////
    /// GET ///
    ///////////
    private val getSubscriptionsFlowNullData: Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> by lazy {
        networkRelayCall.relayGet(
            responseJsonClass = GetSubscriptionsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_SUBSCRIPTIONS,
            relayData = null
        )
    }

    override fun getSubscriptions(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> =
        if (relayData == null) {
            getSubscriptionsFlowNullData
        } else {
            networkRelayCall.relayGet(
                responseJsonClass = GetSubscriptionsRelayResponse::class.java,
                relayEndpoint = ENDPOINT_SUBSCRIPTIONS,
                relayData = relayData
            )
        }

    override fun getSubscriptionById(
        subscriptionId: SubscriptionId,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetSubscriptionRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTION/${subscriptionId.value}",
            relayData = relayData
        )

    override fun getSubscriptionsByContactId(
        contactId: ContactId,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetSubscriptionsRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTIONS/contact/${contactId.value}",
            relayData = relayData
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
