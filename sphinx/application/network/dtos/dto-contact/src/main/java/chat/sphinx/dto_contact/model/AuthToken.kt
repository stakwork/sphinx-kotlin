package chat.sphinx.dto_contact.model

inline class AuthToken(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthToken cannot be empty"
        }
    }
}
