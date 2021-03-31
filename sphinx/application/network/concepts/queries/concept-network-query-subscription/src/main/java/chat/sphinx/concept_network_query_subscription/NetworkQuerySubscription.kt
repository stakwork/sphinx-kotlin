package chat.sphinx.concept_network_query_subscription

import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQuerySubscription {

    ///////////
    /// GET ///
    ///////////
    abstract fun getSubscriptions(): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>>

    abstract fun getSubscriptions(
        authorizationToken: AuthorizationToken,
        relayUrl: RelayUrl,
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>>

    abstract fun getSubscriptionById(
        subscriptionId: SubscriptionId
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>>

    abstract fun getSubscriptionById(
        authorizationToken: AuthorizationToken,
        relayUrl: RelayUrl,
        subscriptionId: SubscriptionId,
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>>

    abstract fun getSubscriptionsByContactId(
        contactId: ContactId
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>>

    abstract fun getSubscriptionsByContactId(
        authorizationToken: AuthorizationToken,
        relayUrl: RelayUrl,
        contactId: ContactId,
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>>

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