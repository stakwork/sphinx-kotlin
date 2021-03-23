package chat.sphinx.feature_network_query_lightning

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.channel.ChannelDto
import chat.sphinx.concept_network_query_lightning.model.channel.ChannelsDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoicesDto
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.retrieveRelayUrlAndJavaWebToken
import chat.sphinx.feature_network_query_lightning.model.GetChannelsRelayResponse
import chat.sphinx.feature_network_query_lightning.model.GetInvoicesRelayResponse
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.network_relay_call.RelayCall
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class NetworkQueryLightningImpl(
    private val dispatchers: CoroutineDispatchers,
    private val moshi: Moshi,
    private val networkClient: NetworkClient,
    private val relayDataHandler: RelayDataHandler
): NetworkQueryLightning() {

    companion object {
        private const val ENDPOINT_INVOICES = "/invoices"
        private const val ENDPOINT_INVOICES_CANCEL = "$ENDPOINT_INVOICES/cancel"
        private const val ENDPOINT_PAYMENT = "/payment"
        private const val ENDPOINT_PAYMENTS = "${ENDPOINT_PAYMENT}s"
        private const val ENDPOINT_CHANNELS = "/channels"
        private const val ENDPOINT_BALANCE = "/balance"
        private const val ENDPOINT_BALANCE_ALL = "$ENDPOINT_BALANCE/all"
        private const val ENDPOINT_GET_INFO = "/getinfo"
        private const val ENDPOINT_LOGS = "/logs"
        private const val ENDPOINT_INFO = "/info"
        private const val ENDPOINT_ROUTE = "/route"
        private const val ENDPOINT_QUERY_ONCHAIN_ADDRESS = "/query/onchain_address"
        private const val ENDPOINT_UTXOS = "/utxos"
    }

    ///////////
    /// GET ///
    ///////////
//    app.get('/invoices', invoices.listInvoices)
    override fun getInvoices(): Flow<LoadResponse<InvoicesDto, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndJavaWebToken().let { response ->
            @Exhaustive
            when (response) {
                is KotlinResponse.Error -> {
                    emit(response)
                }
                is KotlinResponse.Success -> {
                    emitAll(
                        getInvoices(response.value.first, response.value.second)
                    )
                }
            }
        }
    }

    override fun getInvoices(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<InvoicesDto, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetInvoicesRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_INVOICES
        )

    override fun getChannels(): Flow<LoadResponse<ChannelsDto, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndJavaWebToken().let { response ->
            @Exhaustive
            when (response) {
                is KotlinResponse.Error -> {
                    emit(response)
                }
                is KotlinResponse.Success -> {
                    emitAll(
                        getChannels(response.value.first, response.value.second)
                    )
                }
            }
        }
    }

    override fun getChannels(
        javaWebToken: JavaWebToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<ChannelsDto, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = javaWebToken,
            moshi = moshi,
            adapterClass = GetChannelsRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_CHANNELS
        )

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