package chat.sphinx.concept_repository_connect_manager.model

import chat.sphinx.example.wrapper_mqtt.TribeMember
import chat.sphinx.example.wrapper_mqtt.TribeMembersResponse

sealed class ConnectionManagerState {

    object OwnerRegistered: ConnectionManagerState()
    data class MnemonicWords(val words: String): ConnectionManagerState()
    data class UserState(val userState: String): ConnectionManagerState()
    data class SignedChallenge(val authToken: String): ConnectionManagerState()
    data class ErrorMessage(val message: String): ConnectionManagerState()
    data class TribeMembersList(val tribeMembers: TribeMembersResponse): ConnectionManagerState()
}
