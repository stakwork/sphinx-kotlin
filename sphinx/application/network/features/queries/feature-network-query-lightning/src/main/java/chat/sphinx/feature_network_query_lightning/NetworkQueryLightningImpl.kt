package chat.sphinx.feature_network_query_lightning

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceAllDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_lightning.model.channel.ChannelsDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoicesDto
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.concept_relay.retrieveRelayUrlAndAuthorizationToken
import chat.sphinx.feature_network_query_lightning.model.GetBalanceAllRelayResponse
import chat.sphinx.feature_network_query_lightning.model.GetBalanceRelayResponse
import chat.sphinx.feature_network_query_lightning.model.GetChannelsRelayResponse
import chat.sphinx.feature_network_query_lightning.model.GetInvoicesRelayResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.network_relay_call.RelayCall
import chat.sphinx.wrapper_relay.AuthorizationToken
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
    override fun getInvoices(): Flow<LoadResponse<InvoicesDto, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndAuthorizationToken().let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emitAll(
                        getInvoices(response.value.first, response.value.second)
                    )
                }
            }
        }
    }

    override fun getInvoices(
        authorizationToken: AuthorizationToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<InvoicesDto, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = authorizationToken,
            moshi = moshi,
            adapterClass = GetInvoicesRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_INVOICES
        )

    override fun getChannels(): Flow<LoadResponse<ChannelsDto, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndAuthorizationToken().let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emitAll(
                        getChannels(response.value.first, response.value.second)
                    )
                }
            }
        }
    }

    override fun getChannels(
        authorizationToken: AuthorizationToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<ChannelsDto, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = authorizationToken,
            moshi = moshi,
            adapterClass = GetChannelsRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_CHANNELS
        )

    override fun getBalance(): Flow<LoadResponse<BalanceDto, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndAuthorizationToken().let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emitAll(
                        getBalance(response.value.first, response.value.second)
                    )
                }
            }
        }
    }

    override fun getBalance(
        authorizationToken: AuthorizationToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<BalanceDto, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = authorizationToken,
            moshi = moshi,
            adapterClass = GetBalanceRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_BALANCE
        )

    override fun getBalanceAll(): Flow<LoadResponse<BalanceAllDto, ResponseError>> = flow {
        relayDataHandler.retrieveRelayUrlAndAuthorizationToken().let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    emit(response)
                }
                is Response.Success -> {
                    emitAll(
                        getBalanceAll(response.value.first, response.value.second)
                    )
                }
            }
        }
    }

    override fun getBalanceAll(
        authorizationToken: AuthorizationToken,
        relayUrl: RelayUrl
    ): Flow<LoadResponse<BalanceAllDto, ResponseError>> =
        RelayCall.Get.execute(
            dispatchers = dispatchers,
            jwt = authorizationToken,
            moshi = moshi,
            adapterClass = GetBalanceAllRelayResponse::class.java,
            networkClient = networkClient,
            url = relayUrl.value + ENDPOINT_BALANCE_ALL
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