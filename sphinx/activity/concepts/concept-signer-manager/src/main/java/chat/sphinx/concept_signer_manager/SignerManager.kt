package chat.sphinx.concept_signer_manager

abstract class SignerManager {
    abstract fun setupPhoneSigner(mnemonicWords: String?)
    abstract fun setupSignerHardware(signerCallback: SignerCallback)
    abstract fun setWalletDataHandler(walletDataHandlerInstance: Any)
    abstract fun setMoshi(moshiInstance: Any)
    abstract fun setNetworkQueryCrypter(networkQueryCrypterInstance: Any)
}