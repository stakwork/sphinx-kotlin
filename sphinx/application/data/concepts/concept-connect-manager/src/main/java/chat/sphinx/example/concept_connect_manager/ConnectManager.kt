package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.ConnectionState
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
        contacts: HashMap<String, Int>?,
    )
    abstract fun sendKeyExchangeOnionMessage(
        keyExchangeMessage: String,
        hops: String,
        walletMnemonic: WalletMnemonic,
        okKey: String
    )
    abstract fun setLspIp(ip: String)
    abstract fun retrieveLspIp(): String?

}



