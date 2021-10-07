package chat.sphinx.key_restore

sealed class KeyRestoreResponse {

    object Success: KeyRestoreResponse()

    sealed class NotifyState: KeyRestoreResponse() {
        object EncryptingRelayUrl: NotifyState()
        object EncryptingJavaWebToken: NotifyState()
        object EncryptingKeysWithUserPin: NotifyState()
    }

    sealed class Error: KeyRestoreResponse() {
        object KeysThatWereSetDidNotMatch: Error()
        object KeysAlreadyPresent: Error()
        object PrivateKeyWasEmpty: Error()
        object PublicKeyWasEmpty: Error()
        object InvalidUserPin: Error()
    }
}