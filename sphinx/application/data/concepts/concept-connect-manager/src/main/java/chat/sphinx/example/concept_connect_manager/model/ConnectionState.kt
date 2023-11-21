package chat.sphinx.example.concept_connect_manager.model

import chat.sphinx.wrapper_contact.NewContact

sealed class ConnectionState {

    data class MnemonicWords(val words: String): ConnectionState()
    data class OkKey(val okKey: String): ConnectionState()
    data class MqttMessage(val message: String): ConnectionState()
    data class Contact(val contact: NewContact): ConnectionState()
}