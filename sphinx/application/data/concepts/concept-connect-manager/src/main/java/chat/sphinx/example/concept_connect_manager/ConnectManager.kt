package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.wrapper_lightning.WalletMnemonic
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectManager {

    abstract val connectionStateStateFlow: StateFlow<ConnectionState?>

    abstract fun setLspIp(ip: String)

    abstract fun retrieveLspIp(): String?

    abstract suspend fun generateMnemonic(
        mnemonicWords: String?,
    ): Pair<String?, WalletMnemonic?>

    abstract suspend fun generateXPub(
        seed: String,
        time: String,
        network: String
    ): String?

    abstract suspend fun generatePubKeyFromSeed(
        seed: String,
        index: UInt,
        time: String,
        network: String
    ): String?

    abstract fun initializeMqttAndSubscribe(
        serverUri: String,
        mnemonicWords: WalletMnemonic,
        okKey: String,
        contacts: HashMap<String, Int>,
    )

    abstract fun createAccount()

    abstract fun createContact(
        alias: String,
        lightningNodePubKey: String,
        lightningRouteHint: String,
        index: Long,
        walletMnemonic: WalletMnemonic,
        senderLspPubKey: String
    )

    abstract fun sendKeyExchangeOnionMessage(
        keyExchangeMessage: String,
        hops: String,
        walletMnemonic: WalletMnemonic,
        okKey: String
    )

}



