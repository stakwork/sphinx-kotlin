package chat.sphinx.wrapper_meme_server

@Suppress("NOTHING_TO_INLINE")
inline fun String.toAuthenticationToken(): AuthenticationToken? =
    try {
        AuthenticationToken(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val AuthenticationToken.headerKey: String
    get() = AuthenticationToken.HEADER_KEY

inline val AuthenticationToken.headerValue: String
    get() = String.format(AuthenticationToken.HEADER_VALUE, value)

@JvmInline
value class AuthenticationToken(val value: String) {

    companion object {
        const val HEADER_KEY = "Authorization"
        const val HEADER_VALUE = "Bearer %s"
    }

    init {
        require(value.isNotEmpty()) {
            "AuthenticationToken cannot be empty"
        }
    }
}
