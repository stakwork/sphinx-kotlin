package chat.sphinx.concept_repository_connect_manager

import chat.sphinx.concept_repository_connect_manager.model.ConnectionManagerState
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_contact.NewContact
import kotlinx.coroutines.flow.MutableStateFlow

interface ConnectManagerRepository {

    val connectionManagerState: MutableStateFlow<ConnectionManagerState?>

    fun createOwnerAccount()
    fun createContact(contact: NewContact)
    fun connectAndSubscribeToMqtt(userState: ByteArray?) {}
    fun setLspIp(lspIp: String)
    suspend fun createOwnerWithOkKey(okKey: String) {}
    suspend fun updateLspAndOwner(data: String) {}
    suspend fun sendKeyExchange(
        index: Int,
        contactRouteHint: LightningRouteHint?,
        returnConfirmation: Boolean
    ) {}
    suspend fun handleKeyExchangeMessage(json: String) {}
    suspend fun updateContactDetails(json: String) {}




}