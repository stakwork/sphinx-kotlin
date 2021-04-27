package chat.sphinx.concept_network_query_lightning

import chat.sphinx.concept_network_query_lightning.model.balance.BalanceAllDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_lightning.model.channel.ChannelsDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoicesDto
import chat.sphinx.concept_network_query_lightning.model.route.RouteSuccessProbabilityDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryLightning {

    ///////////
    /// GET ///
    ///////////
    abstract fun getInvoices(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<InvoicesDto, ResponseError>>

    abstract fun getChannels(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<ChannelsDto, ResponseError>>

    abstract fun getBalance(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<BalanceDto, ResponseError>>

    abstract fun getBalanceAll(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<BalanceAllDto, ResponseError>>

    abstract fun checkRoute(
        publicKey: LightningNodePubKey,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>>

    abstract fun checkRoute(
        routeHint: LightningRouteHint,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>>

    abstract fun checkRoute(
        publicKey: LightningNodePubKey,
        routeHint: LightningRouteHint,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>>

    abstract fun checkRoute(
        chatId: ChatId,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>>

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

    //////////////
    /// DELETE ///
    //////////////
}