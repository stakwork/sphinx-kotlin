package chat.sphinx.example.concept_connect_manager.model

import chat.sphinx.wrapper_contact.NewContact

sealed class ConnectionState {

    data class MnemonicWords(val words: String): ConnectionState()
    data class OkKey(val okKey: String): ConnectionState()
    data class OwnerRegistered(val message: String): ConnectionState()
    data class ContactRegistered(val index: Int, val message: String): ConnectionState()
    data class NewContactRegistered(val contact: NewContact, val generatedContactRouteHint: String): ConnectionState()
    data class KeyExchangeMessage(val message: String): ConnectionState()
    data class KeySend(val index: Int, val message: String, val rHash: String): ConnectionState()
    data class OnionMessage(val index: Int, val payload: ByteArray?): ConnectionState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OnionMessage

            if (index != other.index) return false
            if (payload != null) {
                if (other.payload == null) return false
                if (!payload.contentEquals(other.payload)) return false
            } else if (other.payload != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + (payload?.contentHashCode() ?: 0)
            return result
        }
    }

    data class ErrorMessage(val message: String): ConnectionState()
}