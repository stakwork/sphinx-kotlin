package chat.sphinx.example.concept_connect_manager

import chat.sphinx.wrapper_lightning.WalletMnemonic
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectManager {

    abstract val mnemonicWords: StateFlow<String?>

    abstract suspend fun generateAndPersistMnemonic(
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


}



