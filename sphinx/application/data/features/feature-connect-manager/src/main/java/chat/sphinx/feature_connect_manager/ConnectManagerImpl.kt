package chat.sphinx.feature_connect_manager

import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.wrapper_lightning.WalletMnemonic

class ConnectManagerImpl(
    private val walletMnemonic: WalletMnemonic
) : ConnectManager() {

    override suspend fun getStoredMnemonicAndSeed(): Pair<String?, WalletMnemonic?> {
        TODO("Not yet implemented")
    }
}