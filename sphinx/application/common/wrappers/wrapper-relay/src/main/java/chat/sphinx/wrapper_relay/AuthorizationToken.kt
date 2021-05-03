package chat.sphinx.wrapper_relay

inline class AuthorizationToken(val value: String) {

    companion object {
        const val AUTHORIZATION_HEADER = "X-User-Token"
    }

    init {
        require(value.isNotEmpty()) {
            "AuthorizationToken cannot be empty"
        }
    }
}
