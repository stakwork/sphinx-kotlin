package chat.sphinx.concept_network_query_subscription

import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQuerySubscription {

    ///////////
    /// GET ///
    ///////////
    abstract fun getSubscriptions(): Flow<KotlinResponse<List<SubscriptionDto>>>

    abstract fun getSubscriptions(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
    ): Flow<KotlinResponse<List<SubscriptionDto>>>

    abstract fun getSubscriptionById(
        subscriptionId: SubscriptionId
    ): Flow<KotlinResponse<SubscriptionDto>>

    abstract fun getSubscriptionById(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        subscriptionId: SubscriptionId,
    ): Flow<KotlinResponse<SubscriptionDto>>

    abstract fun getSubscriptionsByContactId(
        contactId: ContactId
    ): Flow<KotlinResponse<List<SubscriptionDto>>>

    abstract fun getSubscriptionsByContactId(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
        contactId: ContactId,
    ): Flow<KotlinResponse<List<SubscriptionDto>>>

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