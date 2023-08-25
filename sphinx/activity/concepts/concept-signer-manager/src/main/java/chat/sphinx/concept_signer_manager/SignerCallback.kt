package chat.sphinx.concept_signer_manager

interface SignerCallback {
    fun checkNetwork(callback: (Boolean) -> Unit)  // Corresponds to ProfileSideEffect.CheckNetwork
    fun signingDeviceNetwork(callback: (String) -> Unit)
    fun signingDevicePassword(networkName: String, callback: (String) -> Unit)
    fun signingDeviceLightningNodeUrl(callback: (String) -> Unit)
    fun signingDeviceCheckBitcoinNetwork(network: (String) -> Unit, linkSigningDevice: (Boolean) -> Unit)
    fun failedToSetupSigningDevice(message: String)  // Corresponds to ProfileSideEffect.FailedToSetupSigningDevice
    fun sendingSeedToHardware()  // Corresponds to ProfileSideEffect.SendingSeedToHardware
}