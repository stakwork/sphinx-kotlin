package chat.sphinx.concept_signer_manager

abstract class SignerManager {
    abstract fun setupPhoneSigner(mnemonicWords: String?, signerCallback: SignerCallback)
    abstract fun setupSignerHardware(signerCallback: SignerCallback)
    abstract fun setWalletDataHandler(walletDataHandlerInstance: Any)
    abstract fun setMoshi(moshiInstance: Any)
    abstract fun setNetworkQueryCrypter(networkQueryCrypterInstance: Any)

    abstract fun setSeedFromGlyph(mqtt: String, network: String, relay: String)

}