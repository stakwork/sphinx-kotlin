package chat.sphinx.example.concept_connect_manager.model

import chat.sphinx.wrapper_contact.NewContact

sealed class ConnectionState {

    data class MnemonicWords(val words: String): ConnectionState()
    data class OkKey(val okKey: String): ConnectionState()
    data class OwnerRegistered(val message: String): ConnectionState()
    data class ContactRegistered(val index: Int, val message: String): ConnectionState()
    data class NewContactRegistered(val contact: NewContact): ConnectionState()
}