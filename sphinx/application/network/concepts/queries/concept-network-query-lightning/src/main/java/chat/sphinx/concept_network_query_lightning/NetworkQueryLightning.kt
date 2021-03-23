package chat.sphinx.concept_network_query_lightning

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
    // TODO: Is there pagination??
    abstract fun getInvoices(): Flow<LoadResponse<InvoicesDto, ResponseError>>
    abstract fun getInvoices(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<InvoicesDto, ResponseError>>

//    app.get('/channels', details.getChannels)
//    app.get('/balance', details.getBalance)
//    app.get('/balance/all', details.getLocalRemoteBalance)
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