package chat.sphinx.concept_repository_lightning

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_lightning.LightningServiceProvider
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_lightning.NodeBalanceAll
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface LightningRepository {
    val networkRefreshBalance: MutableStateFlow<Long?>

    suspend fun getAccountBalance(): StateFlow<NodeBalance?>

    suspend fun getAccountBalanceAll(
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<NodeBalanceAll, ResponseError>>

    suspend fun updateLSP(lsp: LightningServiceProvider)
    suspend fun retrieveLSP(): Flow<LightningServiceProvider>
}
