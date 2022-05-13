package chat.sphinx.concept_network_query_subscription

import chat.sphinx.concept_network_query_subscription.model.PostSubscriptionDto
import chat.sphinx.concept_network_query_subscription.model.PutSubscriptionDto
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQuerySubscription {

    ///////////
    /// GET ///
    ///////////
    abstract fun getSubscriptions(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>>

    abstract fun getSubscriptionById(
        subscriptionId: SubscriptionId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>>

    abstract fun getSubscriptionsByContactId(
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>>

    ///////////
    /// PUT ///
    ///////////
    abstract fun putSubscription(
        subscriptionId: SubscriptionId,
        putSubscriptionDto: PutSubscriptionDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>>

    abstract fun putPauseSubscription(
        subscriptionId: SubscriptionId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>>

    abstract fun putRestartSubscription(
        subscriptionId: SubscriptionId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>>

    ////////////
    /// POST ///
    ////////////
//    app.post('/subscriptions', subcriptions.createSubscription)
    abstract fun postSubscription(
        postSubscriptionDto: PostSubscriptionDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>>

    //////////////
    /// DELETE ///
    //////////////
    abstract fun deleteSubscription(
        subscriptionId: SubscriptionId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>
}