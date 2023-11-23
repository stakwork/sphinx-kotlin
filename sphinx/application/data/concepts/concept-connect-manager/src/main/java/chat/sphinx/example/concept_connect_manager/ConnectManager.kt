package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
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

    abstract fun connectToMQTT(
        serverURI: String,
        clientId: String,
        key: String,
        password: String,
        okKey: String,
        index: Int
    )

    abstract fun createAccount()

    abstract fun createContact(
        alias: String,
        lightningNodePubKey: String,
        lightningRouteHint: String,
        index: Long,
        walletMnemonic: WalletMnemonic,
        senderOkKey: String,
        senderRouteHint: String,
        senderAlias: String,
        senderPic: String
    )

}



