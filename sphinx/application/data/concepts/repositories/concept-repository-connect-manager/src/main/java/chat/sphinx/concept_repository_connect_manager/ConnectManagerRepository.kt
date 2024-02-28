package chat.sphinx.concept_repository_connect_manager

import chat.sphinx.concept_repository_connect_manager.model.ConnectionManagerState
import chat.sphinx.concept_repository_connect_manager.model.NetworkStatus
import chat.sphinx.wrapper_contact.NewContact
import kotlinx.coroutines.flow.MutableStateFlow

interface ConnectManagerRepository {

    val connectionManagerState: MutableStateFlow<ConnectionManagerState?>
    val networkStatus: MutableStateFlow<NetworkStatus>

    fun createOwnerAccount()
    fun createContact(contact: NewContact)
    fun connectAndSubscribeToMqtt(userState: String?) {}
    fun setLspIp(lspIp: String)
    fun singChallenge(challenge: String)
    fun createInvite(nickname: String, welcomeMessage: String, sats: Long)

    fun joinTribe(
        tribeHost: String,
        tribePubKey: String,
        tribeRouteHint: String,
        tribeName: String,
        isPrivate: Boolean
    )

    fun getTribeMembers(
        tribeServerPubKey: String,
        tribePubKey: String
    )
    suspend fun updateLspAndOwner(data: String) {}

}