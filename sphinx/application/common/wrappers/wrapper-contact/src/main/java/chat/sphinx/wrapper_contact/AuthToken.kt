package chat.sphinx.wrapper_contact

inline class AuthToken(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthToken cannot be empty"
        }
    }
}
