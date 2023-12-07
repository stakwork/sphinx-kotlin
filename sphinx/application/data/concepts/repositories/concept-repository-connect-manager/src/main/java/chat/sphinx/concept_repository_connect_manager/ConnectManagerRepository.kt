package chat.sphinx.concept_repository_connect_manager

import chat.sphinx.concept_repository_connect_manager.model.ConnectionManagerState
import kotlinx.coroutines.flow.MutableStateFlow

interface ConnectManagerRepository {

    val connectionManagerState: MutableStateFlow<ConnectionManagerState?>


    fun setLspIp(lspIp: String)
    fun createOwnerAccount()
    suspend fun createOwnerWithOkKey(okKey: String) {}
    suspend fun updateLspAndOwner(data: String) {}


}