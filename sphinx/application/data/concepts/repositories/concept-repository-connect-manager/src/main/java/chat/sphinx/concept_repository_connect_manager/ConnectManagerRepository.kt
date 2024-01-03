package chat.sphinx.concept_repository_connect_manager

import chat.sphinx.concept_repository_connect_manager.model.ConnectionManagerState
import chat.sphinx.wrapper_contact.NewContact
import kotlinx.coroutines.flow.MutableStateFlow

interface ConnectManagerRepository {

    val connectionManagerState: MutableStateFlow<ConnectionManagerState?>

    fun createOwnerAccount()
    fun createContact(contact: NewContact)
    fun connectAndSubscribeToMqtt(userState: String?) {}
    fun setLspIp(lspIp: String)
    fun singChallenge(challenge: String)
    suspend fun updateLspAndOwner(data: String) {}

}