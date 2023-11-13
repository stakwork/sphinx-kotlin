package chat.sphinx.example.concept_connect_manager

import chat.sphinx.wrapper_lightning.WalletMnemonic

abstract class ConnectManager {
    abstract suspend fun getStoredMnemonicAndSeed(): Pair<String?, WalletMnemonic?>
}


