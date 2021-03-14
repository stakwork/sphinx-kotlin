package chat.sphinx.feature_network_query_subscription

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_relay.RelayDataHandler
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

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
//    app.get('/subscriptions', subcriptions.getAllSubscriptions)
//    app.get('/subscription/:id', subcriptions.getSubscription)
//    app.get('/subscriptions/contact/:contactId', subcriptions.getSubscriptionsForContact)

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