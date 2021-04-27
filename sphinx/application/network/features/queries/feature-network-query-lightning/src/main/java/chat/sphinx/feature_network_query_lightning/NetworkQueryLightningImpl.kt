package chat.sphinx.feature_network_query_lightning

import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceAllDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_lightning.model.channel.ChannelsDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoicesDto
import chat.sphinx.concept_network_query_lightning.model.route.RouteSuccessProbabilityDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_lightning.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryLightningImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryLightning() {

    companion object {
        private const val ENDPOINT_INVOICES = "/invoices"
        private const val ENDPOINT_INVOICES_CANCEL = "$ENDPOINT_INVOICES/cancel"
        private const val ENDPOINT_CHANNELS = "/channels"
        private const val ENDPOINT_BALANCE = "/balance"
        private const val ENDPOINT_BALANCE_ALL = "$ENDPOINT_BALANCE/all"
        private const val ENDPOINT_ROUTE = "/route"
        private const val ENDPOINT_ROUTE_2 = "/route2"
        private const val ENDPOINT_GET_INFO = "/getinfo"
        private const val ENDPOINT_LOGS = "/logs"
        private const val ENDPOINT_INFO = "/info"
        private const val ENDPOINT_QUERY_ONCHAIN_ADDRESS = "/query/onchain_address"
        private const val ENDPOINT_UTXOS = "/utxos"
    }

    ///////////
    /// GET ///
    ///////////
    override fun getInvoices(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<InvoicesDto, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = GetInvoicesRelayResponse::class.java,
            relayEndpoint = ENDPOINT_INVOICES,
            relayData = relayData
        )

    override fun getChannels(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<ChannelsDto, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = GetChannelsRelayResponse::class.java,
            relayEndpoint = ENDPOINT_CHANNELS,
            relayData = relayData
        )

    override fun getBalance(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<BalanceDto, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = GetBalanceRelayResponse::class.java,
            relayEndpoint = ENDPOINT_BALANCE,
            relayData = relayData
        )

    override fun getBalanceAll(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<BalanceAllDto, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = GetBalanceAllRelayResponse::class.java,
            relayEndpoint = ENDPOINT_BALANCE_ALL,
            relayData = relayData
        )

    override fun checkRoute(
        publicKey: LightningNodePubKey,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>> =
        checkRouteImpl(
            endpoint = ENDPOINT_ROUTE + "?pubkey=${publicKey.value}&route_hint=",
            relayData = relayData
        )

    override fun checkRoute(
        routeHint: LightningRouteHint,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>> =
        checkRouteImpl(
            endpoint = ENDPOINT_ROUTE + "?pubkey=&route_hint=${routeHint.value}",
            relayData = relayData
        )

    override fun checkRoute(
        publicKey: LightningNodePubKey,
        routeHint: LightningRouteHint,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>> =
        checkRouteImpl(
            endpoint = ENDPOINT_ROUTE + "?pubkey=${publicKey.value}&route_hint=${routeHint.value}",
            relayData = relayData
        )

    override fun checkRoute(
        chatId: ChatId,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>> =
        checkRouteImpl(
            endpoint = ENDPOINT_ROUTE_2 + "?chat_id=${chatId.value}",
            relayData = relayData
        )

    private fun checkRouteImpl(
        endpoint: String,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>> =
        networkRelayCall.get(
            jsonAdapter = CheckRouteRelayResponse::class.java,
            relayEndpoint = endpoint,
            relayData = relayData
        )

//    app.get('/getinfo', details.getInfo)
//    app.get('/logs', details.getLogsSince)
//    app.get('/info', details.getNodeInfo)
//    app.get('/route', details.checkRoute)
//    app.get('/query/onchain_address/:app', queries.queryOnchainAddress)
//    app.get('/utxos', queries.listUTXOs)

    ///////////
    /// PUT ///
    ///////////
//    app.put('/invoices', invoices.payInvoice)

    ////////////
    /// POST ///
    ////////////
//    app.post('/invoices', invoices.createInvoice)
//    app.post('/invoices/cancel', invoices.cancelInvoice)
//    app.post('/payment', payments.sendPayment)

    //////////////
    /// DELETE ///
    //////////////
}