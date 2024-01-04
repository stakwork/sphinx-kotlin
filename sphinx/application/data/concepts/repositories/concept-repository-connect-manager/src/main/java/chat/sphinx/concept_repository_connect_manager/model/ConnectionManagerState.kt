package chat.sphinx.concept_repository_connect_manager.model

sealed class ConnectionManagerState {

    object OwnerRegistered: ConnectionManagerState()
    data class MnemonicWords(val words: String): ConnectionManagerState()
    data class UserState(val userState: String): ConnectionManagerState()
    data class SignedChallenge(val authToken: String): ConnectionManagerState()
    data class ErrorMessage(val message: String): ConnectionManagerState()
}
