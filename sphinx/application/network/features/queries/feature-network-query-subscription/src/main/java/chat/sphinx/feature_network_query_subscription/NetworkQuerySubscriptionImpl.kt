package chat.sphinx.feature_network_query_subscription

import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_subscription.model.PostSubscriptionDto
import chat.sphinx.concept_network_query_subscription.model.PutSubscriptionDto
import chat.sphinx.concept_network_query_subscription.model.SubscriptionDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_subscription.model.DeleteSubscriptionRelayResponse
import chat.sphinx.feature_network_query_subscription.model.GetSubscriptionsRelayResponse
import chat.sphinx.feature_network_query_subscription.model.SubscriptionRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
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
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
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
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = SubscriptionRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTION/${subscriptionId.value}",
            relayData = relayData
        )

    override fun getSubscriptionsByContactId(
        contactId: ContactId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<List<SubscriptionDto>, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = GetSubscriptionsRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTIONS/contact/${contactId.value}",
            relayData = relayData
        )

    ///////////
    /// PUT ///
    ///////////
    override fun putSubscription(
        subscriptionId: SubscriptionId,
        putSubscriptionDto: PutSubscriptionDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = SubscriptionRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTION/${subscriptionId.value}",
            requestBodyJsonClass = PutSubscriptionDto::class.java,
            requestBody = putSubscriptionDto,
            relayData = relayData
        )

    override fun putPauseSubscription(
        subscriptionId: SubscriptionId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = SubscriptionRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTION/${subscriptionId.value}/pause",
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    override fun putRestartSubscription(
        subscriptionId: SubscriptionId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> =
        networkRelayCall.relayPut(
            responseJsonClass = SubscriptionRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTION/${subscriptionId.value}/restart",
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    ////////////
    /// POST ///
    ////////////
    override fun postSubscription(
        postSubscriptionDto: PostSubscriptionDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<SubscriptionDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = SubscriptionRelayResponse::class.java,
            relayEndpoint = ENDPOINT_SUBSCRIPTIONS,
            requestBodyJsonClass = PostSubscriptionDto::class.java,
            requestBody = postSubscriptionDto,
            relayData = relayData
        )

    //////////////
    /// DELETE ///
    //////////////
    override fun deleteSubscription(
        subscriptionId: SubscriptionId,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.relayDelete(
            responseJsonClass = DeleteSubscriptionRelayResponse::class.java,
            relayEndpoint = "$ENDPOINT_SUBSCRIPTION/${subscriptionId.value}",
            requestBody = null,
            relayData = relayData
        )
}
