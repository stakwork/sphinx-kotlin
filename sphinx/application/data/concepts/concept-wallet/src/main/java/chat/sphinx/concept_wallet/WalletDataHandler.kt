package chat.sphinx.concept_wallet

import chat.sphinx.wrapper_lightning.WalletMnemonic

/**
 * Persists and retrieves Wallet data to device storage. Implementation
 * requires User to be logged in to work, otherwise `null` and `false` are always
 * returned.
 * */

abstract class WalletDataHandler {
    abstract suspend fun persistWalletMnemonic(mnemonic: WalletMnemonic): Boolean
    abstract suspend fun retrieveWalletMnemonic(): WalletMnemonic?
}
