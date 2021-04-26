package chat.sphinx.concept_repository_lightning

import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_contact.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LightningRepository {
    suspend fun getAccountBalance(): StateFlow<NodeBalance?>
    fun networkRefreshBalance(): Flow<LoadResponse<Boolean, ResponseError>>
    fun networkCheckRoute(chat: Chat?, contact: Contact?): Flow<Boolean>
}
