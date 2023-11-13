package chat.sphinx.feature_connect_manager

import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.wrapper_lightning.WalletMnemonic

class ConnectManagerImpl(
    private val walletDataHandler: WalletDataHandler
) : ConnectManager() {

    override suspend fun getStoredMnemonicAndSeed(): Pair<String?, WalletMnemonic?> {
        TODO("Not yet implemented")
    }
}