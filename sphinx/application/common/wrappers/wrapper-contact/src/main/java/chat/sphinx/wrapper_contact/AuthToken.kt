package chat.sphinx.wrapper_contact

@Suppress("NOTHING_TO_INLINE")
inline fun String.toAuthToken(): AuthToken? =
    try {
        AuthToken(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class AuthToken(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthToken cannot be empty"
        }
    }
}
