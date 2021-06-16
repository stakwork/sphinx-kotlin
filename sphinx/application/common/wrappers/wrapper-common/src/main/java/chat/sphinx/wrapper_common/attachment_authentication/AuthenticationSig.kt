package chat.sphinx.wrapper_common.attachment_authentication

@Suppress("NOTHING_TO_INLINE")
inline fun String.toAuthenticationSig(): AuthenticationSig? =
    try {
        AuthenticationSig(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class AuthenticationSig(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthenticationSig cannot be empty"
        }
    }
}