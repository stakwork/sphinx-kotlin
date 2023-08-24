package chat.sphinx.concept_signer_manager

abstract class SignerManager {
    abstract fun setupPhoneSigner()
    abstract fun setupSignerHardware()
    abstract fun initWalletDataHandler(walletDataHandlerInstance: Any)
    abstract fun initMoshi(moshiInstance: Any)

}