package chat.sphinx.concept_repository_connect_manager.model

import chat.sphinx.wrapper_contact.NewContact

sealed class ConnectionManagerState {

    object OwnerRegistered: ConnectionManagerState()
    data class MnemonicWords(val words: String): ConnectionManagerState()
    data class UserState(val userState: ByteArray): ConnectionManagerState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as UserState

            if (!userState.contentEquals(other.userState)) return false

            return true
        }

        override fun hashCode(): Int {
            return userState.contentHashCode()
        }
    }

    data class ErrorMessage(val message: String): ConnectionManagerState()
}
