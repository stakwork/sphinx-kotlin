package chat.sphinx.wrapper_relay

inline class AuthorizationToken(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthorizationToken cannot be empty"
        }
    }
}
