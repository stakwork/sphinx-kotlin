package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_contact.ContactInfo
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_lightning.WalletMnemonic
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectManager {

    abstract val connectionStateStateFlow: StateFlow<ConnectionState?>

    abstract fun createAccount()
    abstract fun createContact(contact: NewContact)
    abstract fun initializeMqttAndSubscribe(
        serverUri: String,
        mnemonicWords: WalletMnemonic,
        okKey: String,
        contacts: List<ContactInfo>?,
        lspPubKey: LightningNodePubKey?,
        )
    abstract fun sendKeyExchangeOnionMessage(
        keyExchangeMessage: String,
        hops: String,
        walletMnemonic: WalletMnemonic,
        okKey: String
    )
    abstract fun setLspIp(ip: String)
    abstract fun retrieveLspIp(): String?

    abstract fun addListener(listener: ConnectManagerListener): Boolean
    abstract fun removeListener(listener: ConnectManagerListener): Boolean

}

interface ConnectManagerListener {

    fun onMnemonicWords(words: String)
    fun onOkKey(okKey: String)
    fun onOwnerRegistered(message: String)
    fun onNewContactRegistered(index: Int, childPubKey: String, scid: String, contactRouteHint: String)
    fun onKeyExchange(json: String)
    fun onKeyExchangeConfirmation(json: String)

}


