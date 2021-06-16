package chat.sphinx.wrapper_common.attachment_authentication


@Suppress("NOTHING_TO_INLINE")
inline fun String.toAuthenticationToken(): AuthenticationToken? =
    try {
        AuthenticationToken(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class AuthenticationToken(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthenticationToken cannot be empty"
        }
    }
}