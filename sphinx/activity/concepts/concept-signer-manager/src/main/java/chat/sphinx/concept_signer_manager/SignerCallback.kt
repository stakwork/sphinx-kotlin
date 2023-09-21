package chat.sphinx.concept_signer_manager

interface SignerCallback {
    fun checkNetwork(callback: (Boolean) -> Unit)
    fun signingDeviceNetwork(callback: (String) -> Unit)
    fun signingDevicePassword(networkName: String, callback: (String) -> Unit)
    fun signingDeviceLightningNodeUrl(callback: (String) -> Unit)
    fun signingDeviceCheckBitcoinNetwork(network: (String) -> Unit, linkSigningDevice: (Boolean) -> Unit)
    fun failedToSetupSigningDevice(message: String)
    fun showMnemonicToUser(message: String, callback: (Boolean) -> Unit)
    fun sendingSeedToHardware()
    fun signingDeviceSuccessfullySet()
}