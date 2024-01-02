package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.OwnerInfo
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_lightning.WalletMnemonic
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectManager {

    abstract val ownerInfoStateFlow: StateFlow<OwnerInfo?>

    abstract fun createAccount()
    abstract fun createContact(contact: NewContact)
    abstract fun initializeMqttAndSubscribe(
        serverUri: String,
        mnemonicWords: WalletMnemonic,
        ownerInfo: OwnerInfo
    )
    abstract fun sendMessage(
        sphinxMessage: String,
        contactPubKey: String
    )
    abstract fun setLspIp(ip: String)
    abstract fun retrieveLspIp(): String?
    abstract fun addListener(listener: ConnectManagerListener): Boolean
    abstract fun removeListener(listener: ConnectManagerListener): Boolean

}

interface ConnectManagerListener {

    fun onMnemonicWords(words: String)
    fun onOwnerRegistered(okKey: String, routeHint: String)
    fun onNewContactRegistered(msgSender: String)
    fun onTextMessageReceived(
        msg: String,
        msgSender: String?,
        msgType: Int?,
        msgUuid: String?,
        msgIndex: String?,
    )
    fun onUpdateUserState(userState: String)

    fun onReconnectMqtt()
}


