package chat.sphinx.concept_signer_manager

abstract class SignerManager {
    abstract fun setupPhoneSigner(mnemonicWords: String?, signerPhoneCallback: SignerPhoneCallback)
    abstract fun setupSignerHardware(signerHardwareCallback: SignerHardwareCallback)

    abstract fun reconnectMQTT(signerPhoneCallback: SignerPhoneCallback)

    abstract fun setWalletDataHandler(walletDataHandlerInstance: Any)
    abstract fun setMoshi(moshiInstance: Any)
    abstract fun setNetworkQueryCrypter(networkQueryCrypterInstance: Any)
    abstract fun setNetworkQueryContact(networkQueryContactInstance: Any)

    abstract fun setSeedFromGlyph(mqtt: String, network: String, relay: String)

    abstract fun isPhoneSignerSettingUp() : Boolean
    abstract fun persistMnemonic()
    abstract suspend fun getPublicKeyAndRelayUrl(): Pair<String, String>?
    abstract suspend fun checkHasAdmin(checkAdminCallback: CheckAdminCallback)

    abstract fun reset()
}