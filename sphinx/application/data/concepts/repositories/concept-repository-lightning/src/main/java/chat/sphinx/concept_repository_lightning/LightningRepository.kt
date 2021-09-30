package chat.sphinx.concept_repository_lightning

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_lightning.NodeBalanceAll
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LightningRepository {
    val networkRefreshBalance: Flow<LoadResponse<Boolean, ResponseError>>
    suspend fun getAccountBalance(): StateFlow<NodeBalance?>
    suspend fun getAccountBalanceAll(
        relayData: Pair<AuthorizationToken, RelayUrl>? = null
    ): Flow<LoadResponse<NodeBalanceAll, ResponseError>>
}
