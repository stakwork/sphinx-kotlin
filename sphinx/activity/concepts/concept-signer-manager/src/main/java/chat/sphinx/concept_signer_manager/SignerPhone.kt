package chat.sphinx.concept_signer_manager

interface SignerPhone {
    fun showMnemonicToUser(message: String, callback: (Boolean) -> Unit)
    fun phoneSignerSuccessfullySet()
    fun phoneSignerSetupError()
}