package chat.sphinx.concept_network_query_lightning

import chat.sphinx.concept_network_query_lightning.model.balance.BalanceAllDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_lightning.model.channel.ChannelDto
import chat.sphinx.concept_network_query_lightning.model.channel.ChannelsDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoicesDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryLightning {

    ///////////
    /// GET ///
    ///////////
    abstract fun getInvoices(): Flow<LoadResponse<InvoicesDto, ResponseError>>
    abstract fun getInvoices(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
    ): Flow<LoadResponse<InvoicesDto, ResponseError>>

    abstract fun getChannels(): Flow<LoadResponse<ChannelsDto, ResponseError>>
    abstract fun getChannels(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
    ): Flow<LoadResponse<ChannelsDto, ResponseError>>

//    app.get('/balance', details.getBalance)
    abstract fun getBalance(): Flow<LoadResponse<BalanceDto, ResponseError>>
    abstract fun getBalance(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
    ): Flow<LoadResponse<BalanceDto, ResponseError>>

//    app.get('/balance/all', details.getLocalRemoteBalance)
abstract fun getBalanceAll(): Flow<LoadResponse<BalanceAllDto, ResponseError>>
    abstract fun getBalanceAll(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl,
    ): Flow<LoadResponse<BalanceAllDto, ResponseError>>

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